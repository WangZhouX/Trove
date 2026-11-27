package io.trove.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.trove.backend.api.MarketApi;
import io.trove.backend.api.MarketException;
import io.trove.backend.entity.ChainAssetDO;
import io.trove.backend.entity.ChainEventDO;
import io.trove.backend.entity.ConsignmentDO;
import io.trove.backend.entity.CustodyItemDO;
import io.trove.backend.entity.MarketListingDO;
import io.trove.backend.entity.MarketSaleDO;
import io.trove.backend.mapper.ChainAssetMapper;
import io.trove.backend.mapper.ChainEventMapper;
import io.trove.backend.mapper.ConsignmentMapper;
import io.trove.backend.mapper.CustodyItemMapper;
import io.trove.backend.mapper.MarketListingMapper;
import io.trove.backend.mapper.MarketSaleMapper;
import io.trove.backend.service.FinalizedTokenOwnershipService;
import io.trove.backend.service.MarketplaceQueryService;
import io.trove.backend.service.WalletSessionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 从 PostgreSQL 已核验投影组装公共市场，并用 Solana 最终确认的账户读取直接持有。
 * Assembles public reads from verified PostgreSQL projections and resolves direct ownership from finalized Solana accounts.
 */
@Service
public class MarketplaceQueryServiceImpl implements MarketplaceQueryService {

    /** 唯一有效挂单状态和单页上限。 / Active listing state and maximum public page size. */
    private static final String ACTIVE = "ACTIVE";
    private static final int MAX_PAGE_SIZE = 100;
    /** 关联查询分批大小，限制单条 Lambda 查询中的绑定参数。 / Join batches limit bound parameters per Lambda query. */
    private static final int BATCH_SIZE = 250;

    /** 读取已索引的链上资产、事件、挂单和成交。 / Reads indexed assets, events, listings and sales. */
    private final ChainAssetMapper assets;
    private final ChainEventMapper events;
    private final MarketListingMapper listings;
    private final MarketSaleMapper sales;
    /** 补齐核验过的公开实物资料和原寄存品类。 / Sources verified public item information and consignment categories. */
    private final CustodyItemMapper custody;
    private final ConsignmentMapper consignments;
    /** 读取最终确认的直接持有。 / Reads finalized direct wallet holdings. */
    private final FinalizedTokenOwnershipService ownership;

    /**
     * 装配查询来源。 / Connect the backing sources for marketplace queries.
     *
     * @param assets 实物 NFT 绑定 / item-to-NFT bindings
     * @param events 最终确认事件 / finalized event records
     * @param listings 固定价挂单 / fixed-price listings
     * @param sales 最终确认成交 / finalized sales
     * @param custody 实物公开信息来源 / source of public physical-item information
     * @param consignments 原始实物品类来源 / source of original physical item categories
     * @param ownership Solana 钱包持有读取 / Solana wallet holdings reader
     */
    public MarketplaceQueryServiceImpl(ChainAssetMapper assets, ChainEventMapper events, MarketListingMapper listings,
                                       MarketSaleMapper sales, CustodyItemMapper custody, ConsignmentMapper consignments,
                                       FinalizedTokenOwnershipService ownership) {
        this.assets = assets;
        this.events = events;
        this.listings = listings;
        this.sales = sales;
        this.custody = custody;
        this.consignments = consignments;
        this.ownership = ownership;
    }

    /** {@inheritDoc} 数据分页及相关实物使用同一读快照。 / Uses one read snapshot for pagination and item information. */
    @Override
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public MarketApi.PageResult<MarketApi.Listing> listMarket(String network, MarketApi.Category category,
                                                             MarketApi.Sort sort, int page, int size) {
        validateNetwork(network);
        validatePage(page, size);
        if (sort == null) {
            throw invalidRequest();
        }
        if (category != null) {
            return categoryPage(network, category, sort, page, size);
        }

        // 全部品类直接由数据库分页；仅查询当前页关联的公开资料。
        // Page all categories in the database and hydrate only the item information for that page.
        var listingPage = listings.selectPage(new Page<>(page, size), sortedListings(network, sort, null));
        var catalog = catalogEntries(network, listingPage.getRecords().stream().map(MarketListingDO::itemId).toList());
        var outward = listingPage.getRecords().stream()
                .map(listing -> publicListing(requireCatalog(catalog, listing.itemId()), listing)).toList();
        return new MarketApi.PageResult<>(outward, page, size, listingPage.getTotal());
    }

    /** {@inheritDoc} 链上绑定和当前有效挂单使用同一读快照。 / Reads the chain binding and active listing in one snapshot. */
    @Override
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public MarketApi.ItemDetail getItem(String network, UUID itemId) {
        validateNetwork(network);
        // 仅公开已完成链上 NFT 绑定的实物，避免暴露尚未铸造的仓库申请。
        // Only expose items bound to a finalized mint, keeping pending warehouse applications private.
        var entry = catalogEntries(network, List.of(itemId)).get(itemId);
        if (entry == null) {
            throw new MarketException(MarketException.Error.ITEM_NOT_FOUND);
        }
        var listing = listings.selectOne(activeListings(network).eq(MarketListingDO::getItemId, itemId));
        var item = entry.item();
        var asset = entry.asset();
        return new MarketApi.ItemDetail(itemId, entry.category(), item.displayTitle(), item.verifiedCondition(),
                item.publicImageUri(), asset.mintAddress(), asset.itemRecordAddress(), asset.collectionMintAddress(),
                item.state(), listing == null ? null : publicListing(entry, listing));
    }

    /** {@inheritDoc} 成交序号按同一网络和藏品唯一，作为分页顺序。 / Sale sequence is unique per network and item. */
    @Override
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public MarketApi.PageResult<MarketApi.Sale> listSales(String network, UUID itemId, int page, int size) {
        validateNetwork(network);
        validatePage(page, size);
        // 只有该网络存在公开 NFT 绑定时，空结果才能表示尚未成交。
        // An empty history means no sales only if the item is published on this network.
        if (!assets.exists(Wrappers.<ChainAssetDO>lambdaQuery().eq(ChainAssetDO::getNetwork, network)
                .eq(ChainAssetDO::getItemId, itemId))) {
            throw new MarketException(MarketException.Error.ITEM_NOT_FOUND);
        }
        var salePage = sales.selectPage(new Page<>(page, size), Wrappers.<MarketSaleDO>lambdaQuery()
                .eq(MarketSaleDO::getNetwork, network).eq(MarketSaleDO::getItemId, itemId)
                .orderByDesc(MarketSaleDO::getSaleNo));
        var eventById = new HashMap<EventKey, ChainEventDO>();
        // 按交易签名批量补齐区块时间，再用事件序号精确匹配同一笔交易内的记录。
        // Fetch timestamps in a batch by signature and match the precise event ordinal within each transaction.
        if (!salePage.getRecords().isEmpty()) {
            var signatures = salePage.getRecords().stream().map(MarketSaleDO::txSignature).distinct().toList();
            var saleEvents = events.selectList(Wrappers.<ChainEventDO>lambdaQuery()
                    .eq(ChainEventDO::getNetwork, network).in(ChainEventDO::getTxSignature, signatures));
            for (var event : saleEvents) {
                eventById.put(new EventKey(event.txSignature(), event.eventIndex()), event);
            }
        }
        var result = salePage.getRecords().stream().map(sale -> {
            var event = eventById.get(new EventKey(sale.txSignature(), sale.eventIndex()));
            return new MarketApi.Sale(sale.listingNo().toString(), sale.saleNo().toString(), sale.saleType(),
                    sale.listingRevision().toString(), sale.sellerWallet(), sale.buyerWallet(),
                    sale.priceLamports().toString(), sale.txSignature(), sale.eventIndex(),
                    event == null ? null : event.blockTime());
        }).toList();
        return new MarketApi.PageResult<>(result, page, size, salePage.getTotal());
    }

    /** {@inheritDoc} 直接持有来自 RPC，同一藏品若同时匹配托管投影则优先展示直接持有。 / Live direct holdings override indexed escrow. */
    @Override
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public MarketApi.Collection getCollection(WalletSessionService.Identity identity) {
        var network = identity.network();
        var wallet = identity.walletAddress();
        validateNetwork(network);

        // 读取失败显示不可用，不能将 RPC 故障误报为用户没有 NFT。
        // Return unavailable on an RPC failure rather than misreporting that the user has no NFTs.
        FinalizedTokenOwnershipService.Ownership held;
        try {
            held = ownership.ownedMints(network, wallet);
        } catch (FinalizedTokenOwnershipService.Unavailable exception) {
            throw new MarketException(MarketException.Error.COLLECTION_OWNERSHIP_UNAVAILABLE);
        }

        // 通过已登记的 Mint 识别 Trove NFT，不使用历史铸造接收者推断当前持有人。
        // Recognize Trove NFTs by the indexed mint and never infer current ownership from the mint recipient.
        var ownedItemIds = ownedItemIds(network, held.mintAddresses());
        var escrow = index(listings.selectList(activeListings(network)
                .eq(MarketListingDO::getSellerWallet, wallet)), MarketListingDO::itemId);
        var itemIds = new LinkedHashSet<>(ownedItemIds);
        itemIds.addAll(escrow.keySet());
        var catalog = catalogEntries(network, List.copyOf(itemIds));
        var result = itemIds.stream().map(itemId -> {
            var entry = requireCatalog(catalog, itemId);
            var item = entry.item();
            // 同一藏品回到钱包但挂单同步尚未完成时，直接持有优先。
            // Prefer the direct balance when an item has returned to the wallet before listing indexing catches up.
            var listing = ownedItemIds.contains(itemId) ? null : publicListing(entry, escrow.get(itemId));
            var holding = listing == null ? MarketApi.Holding.HELD : MarketApi.Holding.LISTED;
            return new MarketApi.CollectionItem(itemId, entry.category(), item.displayTitle(),
                    item.verifiedCondition(), item.publicImageUri(), entry.asset().mintAddress(), holding, listing);
        }).sorted(Comparator.comparing(MarketApi.CollectionItem::title)
                .thenComparing(MarketApi.CollectionItem::itemId)).toList();
        return new MarketApi.Collection(result, Long.toString(held.finalizedSlot()));
    }

    /**
     * 逐批匹配寄存品类，在分类结果上计算精确总数及当前页。
     * Joins consignment categories in bounded batches and counts and pages only matching listings.
     *
     * @param network 目标网络 / target network
     * @param category 要保留的品类 / category to include
     * @param sort 数据库排序规则 / database sort order
     * @param page 从 1 开始的页面 / one-based page number
     * @param size 每页数量 / page size
     * @return 分类准确的市场分页 / market page with exact category counts
     */
    private MarketApi.PageResult<MarketApi.Listing> categoryPage(String network, MarketApi.Category category,
                                                               MarketApi.Sort sort, int page, int size) {
        // 品类在寄存表中，扫描使用挂单排序键续读，控制查询参数和内存并保留 SQL 分页顺序。
        // Categories live on consignments. Resume each batch from the listing sort key to bound queries and memory.
        long offset = (long) (page - 1) * size;
        long total = 0;
        var result = new ArrayList<MarketApi.Listing>(size);
        MarketListingDO last = null;
        List<MarketListingDO> batch;
        do {
            batch = listings.selectPage(new Page<MarketListingDO>(1, BATCH_SIZE, false),
                    sortedListings(network, sort, last)).getRecords();
            var catalog = catalogEntries(network, batch.stream().map(MarketListingDO::itemId).toList());
            for (var listing : batch) {
                var entry = requireCatalog(catalog, listing.itemId());
                if (category.name().equals(entry.category())) {
                    if (total >= offset && result.size() < size) {
                        result.add(publicListing(entry, listing));
                    }
                    total++;
                }
                last = listing;
            }
        } while (batch.size() == BATCH_SIZE);
        return new MarketApi.PageResult<>(List.copyOf(result), page, size, total);
    }

    /**
     * 对指定网络下的有效挂单应用稳定排序及续读边界。
     * Orders active listings with an optional keyset boundary for the specified network.
     *
     * @param network 目标网络 / target network
     * @param sort 排序规则 / sort order
     * @param last 上批最后一条挂单，首批为空 / last listing in the previous batch, null for the first batch
     * @return 带排序和续读的 MyBatis-Plus 条件 / ordered MyBatis-Plus query with an optional keyset boundary
     */
    private static LambdaQueryWrapper<MarketListingDO> sortedListings(String network, MarketApi.Sort sort,
                                                                      MarketListingDO last) {
        var query = activeListings(network);
        // 同一排序值按唯一的实物 UUID 升序，防止批次边界漏掉价格相同的藏品。
        // Break sort ties by unique item UUID so batch boundaries keep listings with equal prices.
        if (last != null) {
            query.and(boundary -> {
                if (sort == MarketApi.Sort.NEWEST) {
                    boundary.lt(MarketListingDO::getUpdatedAt, last.updatedAt()).or(tie -> tie
                            .eq(MarketListingDO::getUpdatedAt, last.updatedAt())
                            .gt(MarketListingDO::getItemId, last.itemId()));
                    return;
                }
                boundary.gt(sort == MarketApi.Sort.PRICE_ASC, MarketListingDO::getPriceLamports, last.priceLamports())
                        .lt(sort == MarketApi.Sort.PRICE_DESC, MarketListingDO::getPriceLamports, last.priceLamports())
                        .or(tie -> tie.eq(MarketListingDO::getPriceLamports, last.priceLamports())
                                .gt(MarketListingDO::getItemId, last.itemId()));
            });
        }
        return query.orderByDesc(sort == MarketApi.Sort.NEWEST, MarketListingDO::getUpdatedAt)
                .orderBy(sort != MarketApi.Sort.NEWEST, sort == MarketApi.Sort.PRICE_ASC,
                        MarketListingDO::getPriceLamports)
                .orderByAsc(MarketListingDO::getItemId);
    }

    /**
     * 通过已索引的 Mint 查找当前钱包直接持有的 Trove 藏品。 / Find held Trove collectibles through their indexed mints.
     *
     * @param network 链上网络 / chain network
     * @param mints RPC 确认的直接持有 Mint / mints held directly according to RPC
     * @return 对应的已建档实物标识 / indexed physical item IDs
     */
    private Set<UUID> ownedItemIds(String network, Set<String> mints) {
        var addresses = List.copyOf(mints);
        var itemIds = new LinkedHashSet<UUID>();
        for (int offset = 0; offset < addresses.size(); offset += BATCH_SIZE) {
            var batch = addresses.subList(offset, Math.min(addresses.size(), offset + BATCH_SIZE));
            var bindings = assets.selectList(Wrappers.<ChainAssetDO>lambdaQuery()
                    .eq(ChainAssetDO::getNetwork, network).in(ChainAssetDO::getMintAddress, batch));
            bindings.forEach(binding -> itemIds.add(binding.itemId()));
        }
        return itemIds;
    }

    /**
     * 批量读取链上绑定、实物和来源品类。 / Reads chain bindings, custody items and source categories in batches.
     *
     * @param network 所查 NFT 所属网络 / network of the queried NFTs
     * @param itemIds 仓库分配的实物标识 / warehouse-assigned item IDs
     * @return 有已核验 NFT 绑定的藏品资料，键为实物 UUID / items with verified NFT bindings keyed by physical UUID
     */
    private Map<UUID, CatalogEntry> catalogEntries(String network, List<UUID> itemIds) {
        var entries = new HashMap<UUID, CatalogEntry>();
        for (int offset = 0; offset < itemIds.size(); offset += BATCH_SIZE) {
            var batch = itemIds.subList(offset, Math.min(itemIds.size(), offset + BATCH_SIZE));
            var bindings = assets.selectList(Wrappers.<ChainAssetDO>lambdaQuery()
                    .eq(ChainAssetDO::getNetwork, network).in(ChainAssetDO::getItemId, batch));
            if (bindings.isEmpty()) {
                continue;
            }
            var physical = custody.selectByIds(bindings.stream().map(ChainAssetDO::itemId).toList());
            var items = index(physical, CustodyItemDO::itemId);
            var origins = index(consignments.selectByIds(
                    physical.stream().map(CustodyItemDO::consignmentId).toList()), ConsignmentDO::consignmentId);
            // 数据库外键保证已索引的 NFT 有实物和来源寄存单。只保留响应需要的品类。
            // Database foreign keys guarantee each indexed NFT has a physical item and origin. Keep just its category.
            for (var binding : bindings) {
                var item = items.get(binding.itemId());
                entries.put(binding.itemId(), new CatalogEntry(binding, item, origins.get(item.consignmentId()).category()));
            }
        }
        return entries;
    }

    /**
     * 创建查询指定网络有效挂单的基础条件。 / Starts a query for active listings on one network.
     *
     * @param network 目标网络 / target network
     * @return 已限制网络和状态的条件 / conditions scoped to network and active state
     */
    private static LambdaQueryWrapper<MarketListingDO> activeListings(String network) {
        return Wrappers.<MarketListingDO>lambdaQuery().eq(MarketListingDO::getNetwork, network)
                .eq(MarketListingDO::getStatus, ACTIVE);
    }

    /**
     * 只提取适合公开展示的字段。 / Extracts only fields suitable for public display.
     *
     * @param entry 已核验的藏品资料 / verified collectible information
     * @param listing 当前有效挂单 / current active listing
     * @return 公开挂单与报价 / public listing and exact quote
     */
    private static MarketApi.Listing publicListing(CatalogEntry entry, MarketListingDO listing) {
        var item = entry.item();
        return new MarketApi.Listing(item.itemId(), entry.category(), item.displayTitle(), item.verifiedCondition(),
                item.publicImageUri(), entry.asset().mintAddress(), listing.listingNo().toString(),
                listing.listingAddress(), listing.sellerWallet(), listing.revision().toString(),
                listing.priceLamports().toString(), listing.updatedAt());
    }

    /**
     * 查询按照唯一 UUID 关联的藏品资料。 / Retrieves required collectible information by its unique UUID.
     *
     * @param catalog 已按实物 UUID 建索引的资料 / collectible information keyed by physical UUID
     * @param itemId 引用的实物 UUID / referenced physical item UUID
     * @return 与调用方投影关联的藏品 / collectible linked to the caller's projection
     * @throws IllegalStateException 挂单引用的藏品投影不完整 / if the referenced listing projection is incomplete
     */
    private static CatalogEntry requireCatalog(Map<UUID, CatalogEntry> catalog, UUID itemId) {
        var entry = catalog.get(itemId);
        if (entry == null) {
            throw new IllegalStateException("Missing catalog record for indexed item " + itemId);
        }
        return entry;
    }

    /**
     * 将批量查询结果按唯一键索引。 / Indexes a batch of results by a unique key.
     *
     * @param rows 查询结果 / query results
     * @param key 行的唯一键 / unique key extractor for a row
     * @param <K> 唯一键类型 / unique key type
     * @param <V> 查询记录类型 / row type
     * @return 由唯一键定位的结果 / results located by unique key
     */
    private static <K, V> Map<K, V> index(List<V> rows, Function<V, K> key) {
        return rows.stream().collect(Collectors.toMap(key, Function.identity()));
    }

    /**
     * 拒绝无效网络，避免意外查询其他链的投影。 / Rejects unsupported networks to avoid unintended projection reads.
     *
     * @param network 页面或会话的网络 / requested or session network
     * @throws MarketException 网络不支持 / if the network is unsupported
     */
    private static void validateNetwork(String network) {
        if (!"devnet".equals(network) && !"localnet".equals(network)) {
            throw invalidRequest();
        }
    }

    /**
     * 校验页码与每页条数。 / Validates page number and size.
     *
     * @param page 从 1 开始的页码 / one-based page number
     * @param size 每页 1 至 100 条 / from 1 to 100 rows per page
     * @throws MarketException 页码或条数无效 / if the page number or size is invalid
     */
    private static void validatePage(int page, int size) {
        if (page < 1 || size < 1 || size > MAX_PAGE_SIZE) {
            throw invalidRequest();
        }
    }

    /** @return 可公开显示的参数错误 / client-safe invalid parameter error */
    private static MarketException invalidRequest() {
        return new MarketException(MarketException.Error.MARKET_INVALID_REQUEST);
    }

    /**
     * 组装公共响应使用的三类信息。 / Sources for assembling a public catalog response.
     *
     * @param asset 链上资产绑定 / on-chain asset binding
     * @param item 实物资料 / physical item information
     * @param category 从来源寄存单取得的品类 / category from the origin consignment
     */
    private record CatalogEntry(ChainAssetDO asset, CustodyItemDO item, String category) {
    }

    /**
     * 同一网络内的事件标识。 / Unique event key within one network.
     *
     * @param signature 交易签名 / transaction signature
     * @param index 交易内从零开始的事件序号 / zero-based event ordinal in the transaction
     */
    private record EventKey(String signature, int index) {
    }
}

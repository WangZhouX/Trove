package io.trove.backend.service;

import io.trove.backend.api.MarketApi;

import java.util.UUID;

/**
 * 从链上最终确认投影读取公开市场数据，结合 RPC 查询当前钱包收藏。
 * Reads public market data from finalized projections and combines RPC data for the current wallet's collection.
 */
public interface MarketplaceQueryService {

    /**
     * 供「市场」展示当前已索引的有效挂单。 / Returns indexed active listings for Market.
     *
     * @param network localnet 或 devnet / localnet or devnet
     * @param category 可选品类，空值表示全部 / optional category, null for all categories
     * @param sort 最新或价格排序 / newest-first or price ordering
     * @param page 从 1 开始的页码 / one-based page number
     * @param size 单页记录上限 / maximum number of results per page
     * @return 挂单页和过滤后的总数 / listing page and count after filtering
     */
    MarketApi.PageResult<MarketApi.Listing> listMarket(String network, MarketApi.Category category,
                                                      MarketApi.Sort sort, int page, int size);

    /**
     * 为详情页加载已绑定链上资产的公开信息。 / Loads public details of an item bound to an on-chain asset.
     *
     * @param network localnet 或 devnet / localnet or devnet
     * @param itemId 藏品的实物 UUID / physical collectible UUID
     * @return 公开资料和当前索引报价 / public details and current indexed quote
     */
    MarketApi.ItemDetail getItem(String network, UUID itemId);

    /**
     * 为详情页「成交记录」查询已最终确认的历史。 / Loads finalized sale history for Item Detail.
     *
     * @param network localnet 或 devnet / localnet or devnet
     * @param itemId 藏品的实物 UUID / physical collectible UUID
     * @param page 从 1 开始的页码 / one-based page number
     * @param size 单页记录上限 / maximum number of results per page
     * @return 成交记录页和成交总数 / sale page and total count
     */
    MarketApi.PageResult<MarketApi.Sale> listSales(String network, UUID itemId, int page, int size);

    /**
     * 「我的收藏」合并最终确认的直接持有与索引的卖家托管记录。
     * Combines finalized direct holdings and indexed seller escrow for My Collection.
     *
     * @param identity 已认证会话绑定的钱包和网络 / authenticated wallet and network from the session
     * @return 钱包收藏及核对直接持有所用区块槽 / collection and finalized slot for direct holdings
     */
    MarketApi.Collection getCollection(WalletSessionService.Identity identity);
}

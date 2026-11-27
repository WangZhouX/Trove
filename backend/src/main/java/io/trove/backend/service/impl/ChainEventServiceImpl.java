package io.trove.backend.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import io.trove.backend.api.ChainTransactionApi;
import io.trove.backend.entity.ChainAssetDO;
import io.trove.backend.entity.ChainEventDO;
import io.trove.backend.entity.MarketListingDO;
import io.trove.backend.entity.MarketSaleDO;
import io.trove.backend.entity.RedemptionReceiptDO;
import io.trove.backend.mapper.ChainAssetMapper;
import io.trove.backend.mapper.ChainEventMapper;
import io.trove.backend.mapper.MarketListingMapper;
import io.trove.backend.mapper.MarketSaleMapper;
import io.trove.backend.mapper.RedemptionReceiptMapper;
import io.trove.backend.service.ChainEventService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * 服务从已索引投影证明事件与当前钱包的关系，再读取交易摘要。
 * Proves how each event relates to the current wallet from indexed projections before reading its summary.
 */
@Service
public class ChainEventServiceImpl extends ServiceImpl<ChainEventMapper, ChainEventDO>
        implements ChainEventService {

    // 铸造接收人、挂单卖家、成交双方、兑换人分别由以下投影确认。
    // These projections establish the mint recipient, listing seller, sale parties and redeemer.
    private final ChainAssetMapper assets;
    private final MarketListingMapper listings;
    private final MarketSaleMapper sales;
    private final RedemptionReceiptMapper redemptions;

    /**
     * 注入用于核对事件参与钱包的投影 Mapper。 / Inject projection mappers that establish event participation.
     *
     * @param assets 铸造绑定 Mapper / mint binding mapper
     * @param listings 挂单 Mapper / listing mapper
     * @param sales 成交 Mapper / sale mapper
     * @param redemptions 兑换凭证 Mapper / redemption receipt mapper
     */
    public ChainEventServiceImpl(
            ChainAssetMapper assets,
            MarketListingMapper listings,
            MarketSaleMapper sales,
            RedemptionReceiptMapper redemptions
    ) {
        this.assets = assets;
        this.listings = listings;
        this.sales = sales;
        this.redemptions = redemptions;
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Optional<ChainTransactionApi.Receipt> findReceipt(String network, String walletAddress, String signature) {
        // 使用同一数据库快照辨认参与者并查找事件。授权以事件序号为单位。
        // Resolve participants and read events in one database snapshot. Authorization is per event ordinal.
        var indexes = visibleEventIndexes(network, walletAddress, signature);
        if (indexes.isEmpty()) {
            return Optional.empty();
        }

        // 按复合位置查找已最终确认事件。JSON 载荷留在服务端，区块槽以文本传输以保持精度。
        // Read finalized events by composite position. Keep JSON payloads server-side and serialize slots as text.
        var summaries = baseMapper.selectList(Wrappers.lambdaQuery(ChainEventDO.class)
                        .eq(ChainEventDO::getNetwork, network)
                        .eq(ChainEventDO::getTxSignature, signature)
                        .in(ChainEventDO::getEventIndex, indexes)
                        .orderByAsc(ChainEventDO::getEventIndex))
                .stream()
                .map(event -> new ChainTransactionApi.EventSummary(event.eventIndex(), event.programId(),
                        event.eventType(), event.slot().toString(), event.blockTime(), event.indexedAt()))
                .toList();
        return summaries.isEmpty()
                ? Optional.empty()
                : Optional.of(new ChainTransactionApi.Receipt(
                        network, signature, ChainTransactionApi.Status.FINALIZED_INDEXED, summaries));
    }

    /**
     * 从可信业务投影收集可见的事件序号。一次交易可能同时包含多个无关钱包的指令。
     * Collects visible event ordinals from trusted business projections. A single transaction can contain instructions
     * involving several unrelated wallets.
     *
     * @param network 会话所绑定的 Solana 网络 / Solana network bound to the session
     * @param walletAddress 已认证的钱包地址 / authenticated wallet address
     * @param signature 待查 Base58 交易签名 / Base58 transaction signature to look up
     * @return 有投影证明当前钱包参与、且属于此交易的去重序号 / deduplicated ordinals supported by projections
     *         and scoped to this transaction
     */
    private Set<Integer> visibleEventIndexes(String network, String walletAddress, String signature) {
        // 稳定的铸造、开单或最近挂单动作、成交、兑换投影给出各自的交易与事件序号。
        // Stable mint, listing creation or latest transition, sale, and redemption projections supply event references.
        Set<Integer> indexes = new HashSet<>();
        for (var asset : assets.selectList(Wrappers.lambdaQuery(ChainAssetDO.class)
                .eq(ChainAssetDO::getNetwork, network)
                .eq(ChainAssetDO::getMintedToWallet, walletAddress)
                .eq(ChainAssetDO::getMintTxSignature, signature))) {
            indexes.add(asset.mintEventIndex());
        }
        for (var listing : listings.selectList(Wrappers.lambdaQuery(MarketListingDO.class)
                .eq(MarketListingDO::getNetwork, network)
                .eq(MarketListingDO::getSellerWallet, walletAddress)
                .and(filter -> filter.eq(MarketListingDO::getOpenTxSignature, signature)
                        .or().eq(MarketListingDO::getLastTxSignature, signature)))) {
            // 一条投影可引用不同交易，只有属于当前签名的序号才授予访问。
            // A projection may refer to different transactions. Grant access only to ordinals for this signature.
            if (signature.equals(listing.openTxSignature())) {
                indexes.add(listing.openEventIndex());
            }
            if (signature.equals(listing.lastTxSignature())) {
                indexes.add(listing.lastEventIndex());
            }
        }
        for (var sale : sales.selectList(Wrappers.lambdaQuery(MarketSaleDO.class)
                .eq(MarketSaleDO::getNetwork, network)
                .eq(MarketSaleDO::getTxSignature, signature)
                .and(filter -> filter.eq(MarketSaleDO::getSellerWallet, walletAddress)
                        .or().eq(MarketSaleDO::getBuyerWallet, walletAddress)))) {
            indexes.add(sale.eventIndex());
        }
        for (var redemption : redemptions.selectList(Wrappers.lambdaQuery(RedemptionReceiptDO.class)
                .eq(RedemptionReceiptDO::getNetwork, network)
                .eq(RedemptionReceiptDO::getRedeemerWallet, walletAddress)
                .eq(RedemptionReceiptDO::getTxSignature, signature))) {
            indexes.add(redemption.eventIndex());
        }
        return indexes;
    }
}

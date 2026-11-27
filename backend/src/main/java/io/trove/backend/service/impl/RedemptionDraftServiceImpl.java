package io.trove.backend.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import io.trove.backend.api.FulfillmentApi;
import io.trove.backend.entity.RedemptionDraftDO;
import io.trove.backend.entity.RedemptionReceiptDO;
import io.trove.backend.entity.ShipmentDO;
import io.trove.backend.mapper.RedemptionDraftMapper;
import io.trove.backend.mapper.RedemptionReceiptMapper;
import io.trove.backend.mapper.ShipmentMapper;
import io.trove.backend.service.RedemptionDraftService;
import io.trove.backend.service.WalletSessionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 将兑换草稿、最终确认凭证和仓库履约关联为兑换人的私有进度。
 * Joins drafts, finalized receipts and fulfillment into progress visible to the redeemer.
 */
@Service
public class RedemptionDraftServiceImpl extends ServiceImpl<RedemptionDraftMapper, RedemptionDraftDO>
        implements RedemptionDraftService {

    /** 关联同一草稿的最终确认凭证。 / Resolves the finalized receipt for the same draft. */
    private final RedemptionReceiptMapper receipts;

    /** 关联该凭证的唯一发货任务。 / Resolves the single shipment task linked to a receipt. */
    private final ShipmentMapper shipments;

    /**
     * 注入查询进度需要的表。 / Injects tables needed for progress lookups.
     *
     * @param receipts 兑换凭证 Mapper / redemption receipt mapper
     * @param shipments 发货任务 Mapper / shipment mapper
     */
    public RedemptionDraftServiceImpl(RedemptionReceiptMapper receipts, ShipmentMapper shipments) {
        this.receipts = receipts;
        this.shipments = shipments;
    }

    /**
     * {@inheritDoc}
     *
     * <p>从草稿出发，按当前页批量加载相关记录。旧草稿不会继承同一藏品新草稿的发货状态。
     * Starts from drafts and batches related records per page. Older drafts do not inherit a later draft's shipment.
     */
    @Override
    @Transactional(readOnly = true)
    public FulfillmentApi.PageResult<FulfillmentApi.RedemptionProgress> listForRedeemer(
            WalletSessionService.Identity identity, int page, int size) {
        FulfillmentViews.validatePage(page, size);
        // 钱包和网络只从已认证会话取得。 / Take the wallet and network only from the authenticated session.
        var draftPage = baseMapper.selectForRedeemer(identity.network(), identity.walletAddress(), page, size);
        if (draftPage.getRecords().isEmpty()) {
            return new FulfillmentApi.PageResult<>(List.of(), page, size, draftPage.getTotal());
        }

        // 先确认草稿引用，再用已确认凭证的实物编号找任务。 / Bind draft references before looking up shipments by finalized item IDs.
        var draftIds = draftPage.getRecords().stream().map(RedemptionDraftDO::draftId).toList();
        var verified = receipts.selectForDrafts(identity.network(), identity.walletAddress(), draftIds);
        var byDraft = verified.stream().collect(Collectors.toMap(RedemptionReceiptDO::draftId, Function.identity()));
        Map<UUID, ShipmentDO> byItem = verified.isEmpty() ? Map.of()
                : shipments.selectForItems(identity.network(), verified.stream().map(RedemptionReceiptDO::itemId).toList())
                .stream().collect(Collectors.toMap(ShipmentDO::itemId, Function.identity()));
        var progress = draftPage.getRecords().stream()
                .map(draft -> progress(draft, byDraft.get(draft.draftId()), byItem))
                .toList();
        return new FulfillmentApi.PageResult<>(progress, page, size, draftPage.getTotal());
    }

    /**
     * 构造对当前兑换人安全的进度输出。 / Builds a redemption progress view safe for the current caller.
     *
     * @param draft 已按钱包和网络过滤的草稿 / draft already scoped by wallet and network
     * @param receipt 该草稿的已验证凭证，未确认时为空 / verified receipt for that draft, null before confirmation
     * @param shipments 已确认实物对应的任务 / shipment tasks assigned to verified items
     * @return 不含私有收件资料的进度 / progress without private recipient details
     */
    private static FulfillmentApi.RedemptionProgress progress(RedemptionDraftDO draft, RedemptionReceiptDO receipt,
                                                              Map<UUID, ShipmentDO> shipments) {
        var shipment = receipt == null ? null : shipments.get(receipt.itemId());
        return new FulfillmentApi.RedemptionProgress(draft.draftId(), draft.itemId(), draft.status(),
                draft.createdAt(), draft.updatedAt(), receipt == null ? null : receipt.txSignature(),
                shipment == null ? null : FulfillmentViews.shipment(shipment));
    }
}

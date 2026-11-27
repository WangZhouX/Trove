package io.trove.backend.service;

import com.baomidou.mybatisplus.spring.service.IService;
import io.trove.backend.api.FulfillmentApi;
import io.trove.backend.entity.RedemptionDraftDO;

/**
 * 声明私有兑换草稿的基础持久化服务契约。
 * Declares the base persistence service contract for private redemption drafts.
 */
public interface RedemptionDraftService extends IService<RedemptionDraftDO> {

    /**
     * 返回当前会话兑换人的草稿和已确认发货进度。
     * Returns drafts and finalized shipment progress for the redeemer in the current session.
     *
     * @param identity 会话钱包和网络 / session wallet and network
     * @param page 从 1 开始的页码 / one-based page number
     * @param size 单页条数 / page size
     * @return 本人在当前网络的兑换进度，无收件字段 / owned redemption progress on the current network without recipient fields
     */
    FulfillmentApi.PageResult<FulfillmentApi.RedemptionProgress> listForRedeemer(
            WalletSessionService.Identity identity, int page, int size);
}

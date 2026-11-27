package io.trove.backend.service;

import io.trove.backend.api.RedemptionDraftCreationApi;
import io.trove.backend.api.RedemptionDraftCreationException;

import java.util.UUID;

/**
 * 受理实物兑换草稿的保存请求。
 * Accepts requests to save physical redemption drafts.
 */
public interface RedemptionDraftCreationService {

    /**
     * 核验链上钱包持有的在库 NFT，然后创建或更新同一份可编辑草稿。
     * Verifies wallet ownership of an in-vault NFT before creating or updating an editable draft.
     *
     * @param identity 会话验证的钱包与网络 / wallet and network verified by the session
     * @param itemId 待兑换的实物 UUID / UUID of the item to redeem
     * @param request 待加密的收件资料 / delivery details to encrypt
     * @return 私有草稿摘要与新建标记 / private draft summary and creation flag
     * @throws RedemptionDraftCreationException 输入、持有状态或服务依赖不符合业务要求时 / when input, ownership or
     *         service dependencies do not meet the workflow requirements
     */
    RedemptionDraftCreationApi.Saved save(
            WalletSessionService.Identity identity, UUID itemId, RedemptionDraftCreationApi.CreateRequest request);
}

package io.trove.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.trove.backend.api.FulfillmentApi;
import io.trove.backend.service.RedemptionDraftService;
import io.trove.backend.service.WalletSessionService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 收藏者在「兑换进度」查看自己的兑换和发货记录。
 * Lets collectors view their own redemption and shipment records on Redemption Progress.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "实物兑换")
public class RedemptionController {

    /** 按会话归属查询兑换草稿和履约进度。 / Reads drafts and fulfillment progress scoped to the session. */
    private final RedemptionDraftService drafts;

    /**
     * 注入兑换进度服务。 / Injects the redemption progress service.
     *
     * @param drafts 兑换草稿及关联状态服务 / service for redemption drafts and related states
     */
    public RedemptionController(RedemptionDraftService drafts) {
        this.drafts = drafts;
    }

    /**
     * 「兑换进度」打开或切换已认证钱包后查询本人记录，待前端联调。
     * Redemption Progress loads owned records when opened or after an authenticated wallet switch. UI integration is pending.
     *
     * @param identity 当前钱包与网络的认证会话 / authenticated session with the current wallet and network
     * @param page 从 1 开始的页码 / one-based page number
     * @param size 单页条数 / page size
     * @return 草稿状态、已确认的交易签名和本人发货进度 / draft states, finalized transaction signatures and owned shipment progress
     */
    @GetMapping("/redemptions/me")
    @Operation(summary = "查询我的兑换进度", security = @SecurityRequirement(name = "walletBearer"))
    public FulfillmentApi.PageResult<FulfillmentApi.RedemptionProgress> listMine(
            @Parameter(hidden = true) @AuthenticationPrincipal WalletSessionService.Identity identity,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size) {
        return drafts.listForRedeemer(identity, page, size);
    }
}

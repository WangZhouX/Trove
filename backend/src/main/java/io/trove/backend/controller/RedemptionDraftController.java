package io.trove.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.trove.backend.api.RedemptionDraftCreationApi;
import io.trove.backend.service.RedemptionDraftCreationService;
import io.trove.backend.service.WalletSessionService;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * 为「我的收藏」的「兑换实物」收件表单提供私有草稿写入。
 * Saves private drafts for the Redeem Item form on My Collection.
 */
@RestController
@RequestMapping("/api/v1/items/{itemId}/redemption-drafts")
@Tag(name = "实物兑换")
public class RedemptionDraftController {

    /** 保存兑换草稿的业务服务。 / Business service for saving redemption drafts. */
    private final RedemptionDraftCreationService drafts;

    /**
     * 注入草稿保存服务。
     * Injects the draft-saving service.
     *
     * @param drafts 兑换草稿保存服务 / redemption draft saving service
     */
    public RedemptionDraftController(RedemptionDraftCreationService drafts) {
        this.drafts = drafts;
    }

    /**
     * 「我的收藏」的「兑换实物」表单在持有人保存收件资料时调用，页面使用草稿编号、状态和截止时间。
     * Called when the holder saves delivery information in My Collection → Redeem Item. The page uses the draft ID,
     * state and deadline.
     *
     * @param identity 安全过滤链验证的会话身份 / session identity verified by the security filter chain
     * @param itemId 兑换目标的实物 UUID / UUID of the physical item to redeem
     * @param request 私有收件资料 / private delivery details
     * @return 新草稿返回 201，更新现有可编辑草稿返回 200 / 201 for a new draft or 200 for an editable existing draft
     */
    @PostMapping
    @Operation(summary = "保存实物兑换草稿", security = @SecurityRequirement(name = "walletBearer"),
            responses = {
                @ApiResponse(responseCode = "201", description = "已创建待签名兑换草稿"),
                @ApiResponse(responseCode = "200", description = "已更新已有待签名兑换草稿")
            })
    public ResponseEntity<RedemptionDraftCreationApi.Draft> save(
            @Parameter(hidden = true) @AuthenticationPrincipal WalletSessionService.Identity identity,
            @PathVariable("itemId") UUID itemId,
            @RequestBody RedemptionDraftCreationApi.CreateRequest request) {
        // 只用会话确定归属，摘要也禁止由代理或浏览器保存到缓存。
        // Derive ownership from the session and prevent proxies and browsers from caching the summary.
        var result = drafts.save(identity, itemId, request);
        var status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).cacheControl(CacheControl.noStore()).body(result.draft());
    }
}

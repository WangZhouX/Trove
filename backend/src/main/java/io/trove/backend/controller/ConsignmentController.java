package io.trove.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.trove.backend.api.ConsignmentApi;
import io.trove.backend.service.ConsignmentService;
import io.trove.backend.service.WalletSessionService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

/**
 * 为「寄存」页面受理寄存申请、查询当前钱包的申请与验收进度。
 * Accepts submissions and looks up applications and inspection progress for the current wallet on the Consign page.
 */
@RestController
@RequestMapping("/api/v1/consignments")
@Tag(name = "藏品寄存")
public class ConsignmentController {

    /** 受理寄存和查询进度的业务服务。 / Service for creating and reading consignments. */
    private final ConsignmentService consignments;

    /**
     * 创建藏品寄存控制器。 / Create the collectible consignment controller.
     *
     * @param consignments 寄存业务服务 / consignment workflow service
     */
    public ConsignmentController(ConsignmentService consignments) {
        this.consignments = consignments;
    }

    /**
     * 「寄存」表单在钱包登录后提交。请求重试沿用原 UUID，页面使用返回的寄存单编号和待寄入状态。
     * Called when a signed-in user submits the Consign form. Retries reuse the request UUID. The result supplies the
     * consignment ID and its arrival status.
     *
     * @param identity 安全过滤链验证的会话身份 / verified session identity
     * @param request 寄存资料与重试标识 / consignment details and retry identifier
     * @return 当前寄存单，首次创建为 201，原请求重试为 200 / current consignment, 201 for creation or 200 for replay
     */
    @PostMapping
    @Operation(summary = "提交藏品寄存申请", security = @SecurityRequirement(name = "walletBearer"),
            responses = {
                @ApiResponse(responseCode = "201", description = "已受理寄存申请",
                        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ConsignmentApi.Detail.class))),
                @ApiResponse(responseCode = "200", description = "已返回原申请及当前进度",
                        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ConsignmentApi.Detail.class)))
            })
    public ResponseEntity<ConsignmentApi.Detail> submit(
            @Parameter(hidden = true) @AuthenticationPrincipal WalletSessionService.Identity identity,
            @RequestBody ConsignmentApi.CreateRequest request) {
        // 寄存归属来自会话，重复提交只返回同一申请。 / Use session ownership and return the original record on retry.
        var submission = consignments.submit(identity.walletAddress(), request);
        var detail = submission.consignment();
        if (submission.created()) {
            return ResponseEntity.created(URI.create("/api/v1/consignments/" + detail.consignmentId())).body(detail);
        }
        return ResponseEntity.ok(detail);
    }

    /**
     * 登录后加载「我的寄存」，或在筛选与翻页时重新查询。列表展示申请名称、寄件信息与当前状态。
     * Loads My Consignments after sign-in or a filter or page change. Rows show the title, inbound details and status.
     *
     * @param identity 安全过滤链验证的会话身份 / verified session identity
     * @param status 可选寄存状态 / optional consignment status
     * @param page 从 1 开始的页码 / one-based page number
     * @param size 每页条数，最多 100 / page size, up to 100
     * @return 当前钱包的寄存列表与总数 / consignment page for the current wallet and total count
     */
    @GetMapping("/me")
    @Operation(summary = "查询我的寄存申请", security = @SecurityRequirement(name = "walletBearer"))
    public ConsignmentApi.PageResult listMine(
            @Parameter(hidden = true) @AuthenticationPrincipal WalletSessionService.Identity identity,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        return consignments.listMine(identity.walletAddress(), status, page, size);
    }

    /**
     * 「我的寄存」打开单条申请时，展示本钱包的完整进度、拒收原因或入库实物编号。
     * Shows progress, a rejection reason, or the accepted item ID when the user opens their own consignment.
     *
     * @param identity 安全过滤链验证的会话身份 / verified session identity
     * @param id 寄存单 UUID / consignment UUID
     * @return 本钱包的寄存详情 / details of a consignment belonging to the current wallet
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询我的寄存详情", security = @SecurityRequirement(name = "walletBearer"))
    public ConsignmentApi.Detail getMine(
            @Parameter(hidden = true) @AuthenticationPrincipal WalletSessionService.Identity identity,
            @PathVariable("id") UUID id) {
        return consignments.getMine(identity.walletAddress(), id);
    }
}

package io.trove.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.trove.backend.api.ConsignmentApi;
import io.trove.backend.api.WalletAuthApi;
import io.trove.backend.service.ConsignmentService;
import io.trove.backend.service.WalletSessionService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * 「仓库工作台」的授权入口与收货、验真、退件操作。
 * Provides the Warehouse Workbench access check, receiving, inspection and return operations.
 */
@RestController
@RequestMapping("/api/v1/warehouse")
@Tag(name = "仓库管理")
public class WarehouseController {

    /** 负责仓库队列与寄存状态变更的服务。 / Service for the warehouse queue and consignment transitions. */
    private final ConsignmentService consignments;

    /**
     * 创建仓库控制器。 / Create the warehouse controller.
     *
     * @param consignments 寄存业务服务 / consignment workflow service
     */
    public WarehouseController(ConsignmentService consignments) {
        this.consignments = consignments;
    }

    /**
     * 打开「仓库工作台」时，验证当前钱包仍具备仓库角色并显示当前地址。
     * Verifies the current wallet still has the warehouse role when opening the workbench and displays its address.
     *
     * @param identity 已认证且通过仓库权限检查的会话身份 / authenticated session identity authorized for the warehouse
     * @return 允许进入工作台的地址、网络和角色 / authorized wallet address, network and roles for the workbench
     */
    @GetMapping("/access")
    @Operation(summary = "核验仓库工作台权限", security = @SecurityRequirement(name = "walletBearer"))
    public WalletAuthApi.WarehouseAccess access(
            @Parameter(hidden = true) @AuthenticationPrincipal WalletSessionService.Identity identity) {
        return new WalletAuthApi.WarehouseAccess(identity.walletAddress(), identity.network(), identity.roles());
    }

    /**
     * 库管员进入「入库与验真」或切换条件、翻页时加载寄存队列。
     * Loads the Intake and Inspection queue when a warehouse operator opens it, filters it or changes pages.
     *
     * @param status 可选寄存状态 / optional consignment status
     * @param page 从 1 开始的页码 / one-based page number
     * @param size 每页条数，最多 100 / page size, up to 100
     * @return 当前页寄存单与总数，表格展示寄件与验收进度 / page of consignments and total for the intake table
     */
    @GetMapping("/consignments")
    @Operation(summary = "查询仓库寄存队列", security = @SecurityRequirement(name = "walletBearer"))
    public ConsignmentApi.PageResult listConsignments(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        return consignments.listForWarehouse(status, page, size);
    }

    /**
     * 库管员在「入库与验真」打开寄存单时，读取原寄件信息与当前状态。
     * Retrieves the initial inbound information and current state when a warehouse operator opens a consignment.
     *
     * @param id 寄存单 UUID / consignment UUID
     * @return 用于核对藏品编号、寄件信息和进度的寄存详情 / details for checking the item's identity, parcel and progress
     */
    @GetMapping("/consignments/{id}")
    @Operation(summary = "查询仓库寄存详情", security = @SecurityRequirement(name = "walletBearer"))
    public ConsignmentApi.Detail getConsignment(@PathVariable("id") UUID id) {
        return consignments.getForWarehouse(id);
    }

    /**
     * 「入库与验真」点击「登记收货」时，将待寄入的申请标记为已收货。
     * Marks a pending consignment as received when an operator clicks Record Receipt in Intake and Inspection.
     *
     * @param identity 经仓库权限检查的会话身份 / authenticated session identity with warehouse access
     * @param id 寄存单 UUID / consignment UUID
     * @return 更新后的状态与收货时间 / updated state and receipt timestamp
     */
    @PostMapping("/consignments/{id}/receive")
    @Operation(summary = "登记寄存收货", security = @SecurityRequirement(name = "walletBearer"))
    public ConsignmentApi.Detail receive(
            @Parameter(hidden = true) @AuthenticationPrincipal WalletSessionService.Identity identity,
            @PathVariable("id") UUID id) {
        return consignments.receive(identity.walletAddress(), id);
    }

    /**
     * 「入库与验真」核对已收货实物后确认通过，返回待铸造状态与同事务生成的实物编号。
     * Approves a received item after inspection and returns its pending-mint state and transactionally allocated item ID.
     *
     * @param identity 经仓库权限检查的会话身份 / authenticated session identity with warehouse access
     * @param id 寄存单 UUID / consignment UUID
     * @param request 已核验的标题、品相和内部仓位 / verified title, condition and internal storage location
     * @return 已验收待铸造的寄存单及实物编号 / approved consignment pending mint and its item ID
     */
    @PostMapping("/consignments/{id}/approve")
    @Operation(summary = "确认寄存验收通过", security = @SecurityRequirement(name = "walletBearer"))
    public ConsignmentApi.Detail approve(
            @Parameter(hidden = true) @AuthenticationPrincipal WalletSessionService.Identity identity,
            @PathVariable("id") UUID id,
            @RequestBody ConsignmentApi.ApproveRequest request) {
        return consignments.approve(identity.walletAddress(), id, request);
    }

    /**
     * 「入库与验真」核对已收货实物后驳回，页面展示拒收待退回与拒收原因。
     * Rejects a received item after inspection and returns the pending-return state and rejection reason.
     *
     * @param identity 经仓库权限检查的会话身份 / authenticated session identity with warehouse access
     * @param id 寄存单 UUID / consignment UUID
     * @param request 给寄存人查看的拒收原因 / rejection reason visible to the depositor
     * @return 已登记拒收的寄存单和原因 / rejected consignment details and reason
     */
    @PostMapping("/consignments/{id}/reject")
    @Operation(summary = "登记寄存验真驳回", security = @SecurityRequirement(name = "walletBearer"))
    public ConsignmentApi.Detail reject(
            @Parameter(hidden = true) @AuthenticationPrincipal WalletSessionService.Identity identity,
            @PathVariable("id") UUID id,
            @RequestBody ConsignmentApi.RejectRequest request) {
        return consignments.reject(identity.walletAddress(), id, request);
    }

    /**
     * 「入库与验真」在拒收实物寄回后登记退件，页面更新为已退回。
     * Records a return after the rejected item has been sent back and updates the workbench to Returned.
     *
     * @param identity 经仓库权限检查的会话身份 / authenticated session identity with warehouse access
     * @param id 寄存单 UUID / consignment UUID
     * @param request 可空的退件运单信息 / optional return tracking details
     * @return 已退回的寄存单及退件信息 / returned consignment and tracking details
     */
    @PostMapping("/consignments/{id}/return")
    @Operation(summary = "登记寄存拒收退回", security = @SecurityRequirement(name = "walletBearer"))
    public ConsignmentApi.Detail recordReturn(
            @Parameter(hidden = true) @AuthenticationPrincipal WalletSessionService.Identity identity,
            @PathVariable("id") UUID id,
            @RequestBody(required = false) ConsignmentApi.ReturnRequest request) {
        // 无请求体与未填运单表示同一种操作。 / An omitted body means the same as an empty tracking number.
        var details = request == null ? new ConsignmentApi.ReturnRequest(null) : request;
        return consignments.recordReturn(identity.walletAddress(), id, details);
    }
}

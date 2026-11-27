package io.trove.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.trove.backend.api.FulfillmentApi;
import io.trove.backend.service.ShipmentService;
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

/** 仓库工作台的履约任务查询和操作入口。 / Fulfillment task queries and actions for the Warehouse Workbench. */
@RestController
@RequestMapping("/api/v1/warehouse/shipments")
@Tag(name = "仓库履约")
public class WarehouseShipmentController {

    /** 根据已最终确认凭证管理仓库发货任务。 / Manages shipments backed by finalized receipts. */
    private final ShipmentService shipments;

    /**
     * 注入仓库履约服务。 / Injects the warehouse fulfillment service.
     *
     * @param shipments 履约队列和事件服务 / shipment queue and action service
     */
    public WarehouseShipmentController(ShipmentService shipments) {
        this.shipments = shipments;
    }

    /**
     * 「仓库工作台 › 出库与履约」打开或切换筛选时查询当前网络任务，待前端联调。
     * The Warehouse Workbench Fulfillment queue queries the current network when opened or filtered. UI integration is pending.
     *
     * @param identity 已授权仓管的会话 / authenticated operator session
     * @param status 可选的履约状态 / optional fulfillment status
     * @param page 从 1 开始的页码 / one-based page
     * @param size 当前页条数 / page size
     * @return 任务编号、出库状态、物流信息和分页总量 / task IDs, dispatch states, shipping details and page total
     */
    @GetMapping
    @Operation(summary = "查询仓库发货任务", security = @SecurityRequirement(name = "walletBearer"))
    public FulfillmentApi.PageResult<FulfillmentApi.ShipmentProgress> list(
            @Parameter(hidden = true) @AuthenticationPrincipal WalletSessionService.Identity identity,
            @RequestParam(required = false) FulfillmentApi.ShipmentStatus status,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size) {
        return shipments.listForWarehouse(identity, status, page, size);
    }

    /**
     * 「仓库工作台 › 出库与履约」登记发货、签收或异常处理时调用，待前端联调。
     * The Warehouse Workbench Fulfillment view records dispatch, delivery or exception handling. UI integration is pending.
     *
     * @param identity 已授权仓管的会话 / authenticated operator session
     * @param shipmentId 当前任务编号 / selected shipment ID
     * @param request 带稳定重试键的操作详情 / action details with a stable retry key
     * @return 最新任务进度和首次登记的事件 / latest task progress and the original action
     */
    @PostMapping("/{shipmentId}/events")
    @Operation(summary = "登记仓库履约事件", security = @SecurityRequirement(name = "walletBearer"))
    public FulfillmentApi.EventResult record(
            @Parameter(hidden = true) @AuthenticationPrincipal WalletSessionService.Identity identity,
            @PathVariable UUID shipmentId, @RequestBody FulfillmentApi.RecordEventRequest request) {
        return shipments.recordEvent(identity, shipmentId, request);
    }
}

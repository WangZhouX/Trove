package io.trove.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.trove.backend.api.WarehouseShipmentRecipientApi;
import io.trove.backend.service.WalletSessionService;
import io.trove.backend.service.WarehouseShipmentRecipientService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * 为仓库工作台按单笔任务核对收件资料提供显式入口。
 * Provides an explicit Warehouse Workbench endpoint for checking one shipment's delivery details.
 */
@RestController
@RequestMapping("/api/v1/warehouse/shipments/{shipmentId}/recipient")
@Tag(name = "仓库履约")
public class WarehouseShipmentRecipientController {

    /** 授权查找并解密单笔任务收件资料的业务服务。 / Service that authorizes and decrypts one shipment's delivery details. */
    private final WarehouseShipmentRecipientService recipients;

    /**
     * 装配单笔任务的收件资料查询服务。 / Injects the single-shipment recipient lookup service.
     *
     * @param recipients 收件资料查询 / delivery detail lookup
     */
    public WarehouseShipmentRecipientController(WarehouseShipmentRecipientService recipients) {
        this.recipients = recipients;
    }

    /**
     * 「仓库工作台 › 出库与履约」打开指定任务的收件资料时调用，待前端联调。页面用于核对姓名、电话和地址。
     * Called when an operator explicitly opens the recipient for a task in Warehouse Workbench → Fulfillment.
     * The UI integration is pending. The response supplies name, phone and address for delivery verification.
     *
     * @param identity 已验证的仓管会话 / verified warehouse operator session
     * @param shipmentId 当前网络中的发货任务 UUID / shipment UUID on the current network
     * @return 不允许客户端缓存的单笔任务收件资料 / single-shipment recipient details with caching prohibited
     */
    @GetMapping
    @Operation(summary = "查看发货任务收件资料", security = @SecurityRequirement(name = "walletBearer"))
    public ResponseEntity<WarehouseShipmentRecipientApi.Recipient> get(
            @Parameter(hidden = true) @AuthenticationPrincipal WalletSessionService.Identity identity,
            @PathVariable("shipmentId") UUID shipmentId) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(recipients.get(identity, shipmentId));
    }
}

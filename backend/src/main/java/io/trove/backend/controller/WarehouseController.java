package io.trove.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.trove.backend.api.WalletAuthApi;
import io.trove.backend.service.WalletSessionService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 「仓库工作台」的服务端授权入口，供后续收货与履约流程使用。
 * Provides server-side authorization for the Warehouse Workbench and its future intake and fulfillment flows.
 */
@RestController
@RequestMapping("/api/v1/warehouse")
@Tag(name = "仓库管理")
public class WarehouseController {

    /**
     * 打开「仓库工作台」时，验证当前钱包仍具备仓库角色并显示当前地址。
     * Verifies the current wallet still has the warehouse role when opening the workbench and displays its address.
     *
     * @param identity 已认证且通过仓库权限检查的会话身份 / authenticated session identity authorized for the warehouse
     * @return 允许进入工作台的地址、网络和角色 / authorized wallet address, network and roles for the workbench
     */
    @GetMapping("/access")
    @Operation(summary = "核验仓库工作台权限", security = @SecurityRequirement(name = "walletBearer"))
    public WalletAuthApi.WarehouseAccess access(@AuthenticationPrincipal WalletSessionService.Identity identity) {
        return new WalletAuthApi.WarehouseAccess(identity.walletAddress(), identity.network(), identity.roles());
    }
}

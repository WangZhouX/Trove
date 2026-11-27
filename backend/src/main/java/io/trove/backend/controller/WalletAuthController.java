package io.trove.backend.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 为连接 Phantom 或切换钱包后的登录流程保留挑战与会话入口。
 * Reserves challenge and session entry points for signing in after connecting or switching a Phantom wallet.
 */
@RestController
@RequestMapping("/api/v1/auth/wallet")
@Tag(name = "钱包认证")
public class WalletAuthController {
}

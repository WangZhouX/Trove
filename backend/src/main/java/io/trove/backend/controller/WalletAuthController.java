package io.trove.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.trove.backend.api.WalletAuthApi;
import io.trove.backend.api.WalletAuthException;
import io.trove.backend.service.WalletAuthService;
import io.trove.backend.service.WalletSessionService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 处理顶栏「连接 Phantom」的签名登录和当前会话。
 * Handles signed sign-in and the current session for “Connect Phantom” in the site header.
 */
@RestController
@RequestMapping("/api/v1/auth/wallet")
@Tag(name = "钱包认证")
public class WalletAuthController {

    /** 验签服务和会话管理服务。 / Signature challenge and session lifecycle services. */
    private final WalletAuthService walletAuth;
    private final WalletSessionService sessions;

    /**
     * 创建钱包登录控制器。 / Create the wallet sign-in controller.
     *
     * @param walletAuth 挑战签发和验证服务 / challenge issuance and verification service
     * @param sessions Bearer 会话服务 / bearer session service
     */
    public WalletAuthController(WalletAuthService walletAuth, WalletSessionService sessions) {
        this.walletAuth = walletAuth;
        this.sessions = sessions;
    }

    /**
     * 「连接 Phantom」得到当前公钥后发起，前端用 message 交给钱包签名。
     * Called after “Connect Phantom” obtains the active public key. The browser passes message to the wallet.
     *
     * @param request 钱包地址和当前网络 / wallet address and current network
     * @param origin 浏览器当前站点来源 / current browser origin
     * @return 挑战、原始消息和到期时间 / challenge, exact message and expiration time
     */
    @PostMapping("/challenges")
    @Operation(summary = "申请钱包登录挑战")
    public WalletAuthApi.ChallengeResponse createChallenge(@RequestBody WalletAuthApi.ChallengeRequest request,
            @RequestHeader(value = HttpHeaders.ORIGIN, required = false) String origin) {
        var challenge = walletAuth.issueChallenge(request.walletAddress(), request.network(), origin);
        return new WalletAuthApi.ChallengeResponse(challenge.challengeId(), challenge.nonce(), challenge.message(),
                challenge.expiresAt());
    }

    /**
     * Phantom 批准签名后使用，前端在内存保存 token 并读取钱包和角色。
     * Called after Phantom approves the signature. The browser keeps the token in memory and reads identity and roles.
     *
     * @param request 原挑战材料和钱包签名 / original challenge data and wallet signature
     * @param origin 发起挑战时的浏览器来源 / browser origin that initiated the challenge
     * @return 短期会话凭证、已验证身份和角色 / short-lived session credential, verified identity and roles
     */
    @PostMapping("/sessions")
    @Operation(summary = "验证钱包签名并登录")
    public WalletAuthApi.SessionResponse createSession(@RequestBody WalletAuthApi.SessionRequest request,
            @RequestHeader(value = HttpHeaders.ORIGIN, required = false) String origin) {
        var wallet = walletAuth.verifyChallenge(request.challengeId(), request.walletAddress(), request.network(),
                origin, request.nonce(), request.signature());
        // 验签服务返回时数据库事务已提交，随后签发短期会话。
        // The verification transaction has committed before the session credential is issued.
        var signIn = sessions.issue(wallet);
        var identity = signIn.identity();
        return new WalletAuthApi.SessionResponse(signIn.token(), identity.walletAddress(), identity.network(),
                identity.roles(), identity.expiresAt());
    }

    /**
     * 前端校验当前会话时查询已认证钱包及最新角色。
     * Used by the browser to check its current session identity and current roles.
     *
     * @param identity 安全过滤链验证的会话身份 / session identity verified by the security filter chain
     * @return 当前地址、网络、角色及有效期 / current wallet, network, roles and expiration
     */
    @GetMapping("/sessions/current")
    @Operation(summary = "查询当前钱包会话", security = @SecurityRequirement(name = "walletBearer"))
    public WalletAuthApi.SessionView currentSession(@AuthenticationPrincipal WalletSessionService.Identity identity) {
        return new WalletAuthApi.SessionView(identity.walletAddress(), identity.network(), identity.roles(),
                identity.expiresAt());
    }

    /**
     * 退出或切换钱包时撤销旧会话。 / Revokes the old session on sign-out or wallet switching.
     *
     * @param authorization 当前请求的 Bearer 凭证头 / bearer credential header of the current request
     * @return 撤销后返回无内容 / no content after revocation
     */
    @DeleteMapping("/sessions/current")
    @Operation(summary = "退出当前钱包会话", security = @SecurityRequirement(name = "walletBearer"))
    public ResponseEntity<Void> deleteSession(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        sessions.revoke(authorization);
        return ResponseEntity.noContent().build();
    }

    /**
     * 把认证失败转换成前端可显示的固定错误。 / Maps authentication failures to stable client errors.
     *
     * @param exception 认证失败原因 / authentication failure reason
     * @return HTTP 错误状态和安全提示 / HTTP error status and safe feedback
     */
    @ExceptionHandler(WalletAuthException.class)
    public ResponseEntity<WalletAuthException.Response> authError(WalletAuthException exception) {
        var error = exception.error();
        return ResponseEntity.status(error.status()).body(error.response());
    }

    /**
     * 请求 JSON 或挑战编号无法解析时返回统一的 400 错误。
     * Produces a stable 400 error for malformed JSON or challenge identifiers.
     *
     * @return 固定格式的登录请求错误 / stable sign-in request error
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<WalletAuthException.Response> malformedRequest() {
        var error = WalletAuthException.Error.INVALID_REQUEST;
        return ResponseEntity.status(error.status()).body(error.response());
    }
}

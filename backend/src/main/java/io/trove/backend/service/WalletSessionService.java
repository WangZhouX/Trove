package io.trove.backend.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/** 管理签名登录后短期的 Bearer 会话。 / Manages short-lived bearer sessions after signature verification. */
public interface WalletSessionService {

    /**
     * 只在签名验证及消费挑战提交后签发会话。 / Issues a session only after signature verification and committed consumption.
     *
     * @param verifiedWallet 已验证的会话身份 / verified session identity
     * @return 一次性返回的 Bearer 凭证及当前身份 / bearer credential returned once and the current identity
     */
    SignedIn issue(WalletAuthService.VerifiedWallet verifiedWallet);

    /**
     * 查询有效会话并读取最新角色。 / Resolve a valid session and read its current roles.
     *
     * @param authorization HTTP Authorization 头，允许匿名请求为空 / HTTP Authorization header, nullable for anonymous requests
     * @param origin 浏览器发送的 Origin，若存在则与签名来源完全匹配 / browser Origin, which must match the signed origin when supplied
     * @return 有效会话身份，错误或过期凭证返回空 / current identity, or empty for invalid or expired credentials
     */
    Optional<Identity> authenticate(String authorization, String origin);

    /**
     * 撤销当前会话，重复请求保持已撤销状态。 / Revoke the current session, leaving it revoked on repeated calls.
     *
     * @param authorization 当前请求的 Authorization 头 / current Authorization header
     */
    void revoke(String authorization);

    /**
     * 受保护控制器可读取的认证身份，不含凭证。 / Authenticated identity for protected controllers, without credentials.
     *
     * @param walletAddress 已验证的钱包公钥 / verified wallet public key
     * @param network 签名绑定的链上网络 / chain network bound by the signature
     * @param roles 本次从数据库读取的角色 / roles loaded from the database for this request
     * @param expiresAt Bearer 会话的 UTC 到期时间 / UTC bearer session expiration
     */
    record Identity(String walletAddress, String network, List<String> roles, OffsetDateTime expiresAt) {
    }

    /**
     * 登录成功结果。 / Successful login result.
     *
     * @param token 仅通过登录应答发给客户端的凭证 / credential sent to the client only in the sign-in response
     * @param identity 此会话的身份 / identity of this session
     */
    record SignedIn(String token, Identity identity) {
    }
}

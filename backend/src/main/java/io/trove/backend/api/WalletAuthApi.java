package io.trove.backend.api;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 钱包登录、会话查询和仓库权限查询的 HTTP 数据结构。
 * HTTP request and response types for wallet sign-in, session inspection and warehouse access.
 */
public final class WalletAuthApi {

    private WalletAuthApi() {
    }

    /**
     * 顶栏请求为当前钱包生成签名消息。 / Request a sign-in message for the active wallet from the header.
     *
     * @param walletAddress Base58 编码的 Solana 公钥 / Base58-encoded Solana public key
     * @param network Trove 选择的网络，devnet 或 localnet / selected Trove network, devnet or localnet
     */
    public record ChallengeRequest(String walletAddress, String network) {
    }

    /**
     * 给 Phantom 签名的挑战。 / Challenge to sign with Phantom.
     *
     * @param challengeId 服务端生成的一次性挑战 UUID / server-generated one-time challenge UUID
     * @param nonce Base64url 随机值，回传时保持原样 / Base64url nonce returned unchanged during verification
     * @param message 待签名的 UTF-8 文本，保留原有换行 / UTF-8 message to sign with its original line endings
     * @param expiresAt UTC 到期时间，到期后重新请求挑战 / UTC expiration time, after which a new challenge is required
     */
    public record ChallengeResponse(UUID challengeId, String nonce, String message, OffsetDateTime expiresAt) {
    }

    /**
     * 将钱包签名提交给后端。 / Submit a wallet signature for verification.
     *
     * @param challengeId 此次签名的挑战 UUID / UUID of the signed challenge
     * @param walletAddress 已签名的 Base58 钱包地址 / Base58 address of the signer
     * @param network 原挑战网络 / original challenge network
     * @param nonce 原挑战随机值 / original challenge nonce
     * @param signature 标准 Base64 编码的 64 字节 Ed25519 签名 / standard Base64 of the 64-byte Ed25519 signature
     */
    public record SessionRequest(UUID challengeId, String walletAddress, String network, String nonce, String signature) {
    }

    /**
     * 登录成功后发给前端的会话。 / Browser session issued after successful sign-in.
     *
     * @param token 后续请求的短时 Bearer 凭证，仅登录成功时返回 / short-lived Bearer credential returned only at sign-in
     * @param walletAddress 服务端验证的钱包地址 / wallet address verified by the server
     * @param network 会话绑定的网络 / network bound to the session
     * @param roles 服务端查询的角色代码，普通钱包为空列表 / server-loaded role codes, empty for regular wallets
     * @param expiresAt 会话有效期的 UTC 时间 / session expiration in UTC
     */
    public record SessionResponse(String token, String walletAddress, String network, List<String> roles,
                                  OffsetDateTime expiresAt) {
    }

    /**
     * 登录会话的当前身份与角色。 / Current session identity and roles.
     *
     * @param walletAddress 已验证的钱包公钥 / verified wallet public key
     * @param network 会话绑定的网络 / network bound to the session
     * @param roles 仍有效的数据库角色授权 / currently effective database role grants
     * @param expiresAt 会话有效期的 UTC 时间 / session expiration in UTC
     */
    public record SessionView(String walletAddress, String network, List<String> roles, OffsetDateTime expiresAt) {
    }

    /**
     * 仓库工作台的服务端访问确认。 / Server confirmation of access to the Warehouse Workbench.
     *
     * @param walletAddress 允许访问工作台的钱包地址 / wallet address authorized for the workbench
     * @param network 当前会话网络 / current session network
     * @param roles 当前角色代码，含仓库操作权限 / current role codes, including the warehouse permission
     */
    public record WarehouseAccess(String walletAddress, String network, List<String> roles) {
    }
}

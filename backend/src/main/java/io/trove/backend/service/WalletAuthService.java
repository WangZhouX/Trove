package io.trove.backend.service;

import io.trove.backend.api.WalletAuthException;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 生成并验证 Solana 钱包登录挑战。 / Issues and verifies Solana wallet sign-in challenges.
 */
public interface WalletAuthService {

    /**
     * 为当前钱包生成短时签名消息并保存挑战摘要。 / Issue a short-lived message and persist the challenge digest.
     *
     * @param walletAddress Base58 钱包公钥 / Base58 wallet public key
     * @param network 当前应用网络 / current application network
     * @param origin HTTP Origin 头 / HTTP Origin header
     * @return 提供给 Phantom 的挑战数据 / challenge data to pass to Phantom
     * @throws WalletAuthException 钱包地址、网络或来源无效 / if the address, network or origin is invalid
     */
    Challenge issueChallenge(String walletAddress, String network, String origin);

    /**
     * 验证签名并在同一数据库事务中消费挑战及登记登录时间。
     * Verify the signature and consume the challenge and record the login in one database transaction.
     *
     * @param challengeId 原挑战编号 / original challenge ID
     * @param walletAddress 签名者钱包公钥 / signer wallet public key
     * @param network 原挑战网络 / original challenge network
     * @param origin HTTP Origin 头 / HTTP Origin header
     * @param nonce 发给客户端的随机值 / nonce issued to the client
     * @param signature 标准 Base64 Ed25519 签名 / standard Base64 Ed25519 signature
     * @return 可在事务提交后创建会话的已认证钱包 / verified wallet eligible for a session after commit
     * @throws WalletAuthException 挑战无效或签名校验失败 / if the challenge or signature is rejected
     */
    VerifiedWallet verifyChallenge(UUID challengeId, String walletAddress, String network, String origin,
                                   String nonce, String signature);

    /**
     * 一次性签名挑战。 / One-time challenge to sign.
     *
     * @param challengeId 挑战 UUID / challenge UUID
     * @param nonce Base64url 随机值 / Base64url random nonce
     * @param message 完整待签 UTF-8 消息 / exact UTF-8 message to sign
     * @param expiresAt UTC 到期时间 / UTC expiration
     */
    record Challenge(UUID challengeId, String nonce, String message, OffsetDateTime expiresAt) {
    }

    /**
     * 挑战验证产生的会话身份。 / Session identity established by challenge verification.
     *
     * @param walletAddress 已认证的钱包公钥 / authenticated wallet public key
     * @param network 签名绑定的网络 / network bound in the signature
     * @param origin 签名绑定的来源 / origin bound in the signature
     */
    record VerifiedWallet(String walletAddress, String network, String origin) {
    }
}

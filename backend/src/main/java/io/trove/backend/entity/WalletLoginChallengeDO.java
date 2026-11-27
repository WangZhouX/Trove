package io.trove.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 钱包签名登录的一次性挑战，按钱包、站点和网络限定使用范围。
 * One-time wallet login challenge scoped to a wallet, site and network.
 *
 * @param challengeId 后端生成的 UUID 登录挑战编号。
 *        Server-generated UUID login challenge identifier.
 * @param walletAddress 预期签名的钱包公钥。
 *        Public key of the expected wallet signer.
 * @param network 签名消息绑定的网络，localnet 或 devnet。
 *        Network bound to the signed message, localnet or devnet.
 * @param domain 服务端允许的签名站点域名，可带本地开发端口。
 *        Server-allowed signing domain, optionally including a local development port.
 * @param nonceHash 随机挑战值的 SHA-256 摘要，固定 32 字节。
 *        Fixed 32-byte SHA-256 digest of the random nonce.
 * @param issuedAt 挑战签发时间，API 以 UTC 返回。
 *        Challenge issue time, exposed in UTC by the API.
 * @param expiresAt 挑战到期时间，等于当前时间时已不可用。
 *        Challenge expiration time, exclusive.
 * @param consumedAt 挑战被成功消费的时间，空值表示未消费。
 *        Successful consumption time, null while unused.
 */
@TableName("wallet_login_challenge")
public record WalletLoginChallengeDO(
        @TableId(value = "challenge_id", type = IdType.INPUT) UUID challengeId,
        @TableField("wallet_address") String walletAddress,
        @TableField("network") String network,
        @TableField("domain") String domain,
        @TableField("nonce_hash") byte[] nonceHash,
        @TableField("issued_at") OffsetDateTime issuedAt,
        @TableField("expires_at") OffsetDateTime expiresAt,
        @TableField("consumed_at") OffsetDateTime consumedAt
) {

    /**
     * 供 MyBatis-Plus Lambda 查询读取 {@code challenge_id} 主键列。
     * Exposes the primary-key column {@code challenge_id} for MyBatis-Plus Lambda queries.
     *
     * @return 主键列的当前值。 The current primary-key value.
     */
    public UUID getChallengeId() {
        return challengeId;
    }

    /** @return 用于原子消费时检查有效期的 UTC 时间 / expiration in UTC for the atomic consumption check */
    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    /** @return 用于原子消费时判断是否已用的 UTC 时间 / consumption timestamp for the one-time write condition */
    public OffsetDateTime getConsumedAt() {
        return consumedAt;
    }
}

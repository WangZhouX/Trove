package io.trove.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.OffsetDateTime;

/**
 * 曾发起钱包登录的身份记录。签名验证后才能用于受保护的业务。
 * Identity records for wallets that initiated sign-in. Protected operations require verified signatures.
 *
 * @param walletAddress Solana 钱包公钥的 Base58 文本，大小写敏感。
 *        Case-sensitive Base58 text of a Solana wallet public key.
 * @param createdAt 身份记录创建时间，API 以 UTC 返回。
 *        Identity creation time, exposed in UTC by the API.
 * @param firstVerifiedAt 首次成功验证钱包签名的时间，空值表示尚未验证。
 *        First successful wallet-signature verification time, null until verified.
 * @param lastVerifiedAt 最近一次成功验证钱包签名的时间，空值表示尚未验证。
 *        Most recent successful verification time, null until verified.
 */
@TableName("wallet_account")
public record WalletAccountDO(
        @TableId(value = "wallet_address", type = IdType.INPUT) String walletAddress,
        @TableField("created_at") OffsetDateTime createdAt,
        @TableField("first_verified_at") OffsetDateTime firstVerifiedAt,
        @TableField("last_verified_at") OffsetDateTime lastVerifiedAt
) {

    /**
     * 供 MyBatis-Plus Lambda 查询读取 {@code wallet_address} 主键列。
     * Exposes the primary-key column {@code wallet_address} for MyBatis-Plus Lambda queries.
     *
     * @return 主键列的当前值。 The current primary-key value.
     */
    public String getWalletAddress() {
        return walletAddress;
    }

    /** @return 用于首次验证的条件写入时间 / timestamp used for conditional first-verification writes */
    public OffsetDateTime getFirstVerifiedAt() {
        return firstVerifiedAt;
    }

    /** @return 用于防止后提交请求覆盖较新验证时间 / latest verification timestamp for monotonic updates */
    public OffsetDateTime getLastVerifiedAt() {
        return lastVerifiedAt;
    }
}

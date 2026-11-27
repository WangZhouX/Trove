package io.trove.backend.entity;

import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 曾发起钱包登录的身份记录。签名验证后才能用于受保护的业务。
 * Identity records for wallets that initiated sign-in. Protected operations require verified signatures.
 */
@TableName("wallet_account")
public record WalletAccountDO() {
}

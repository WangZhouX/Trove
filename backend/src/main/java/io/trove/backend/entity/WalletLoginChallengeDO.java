package io.trove.backend.entity;

import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 钱包签名登录的一次性挑战，按钱包、站点和网络限定使用范围。
 * One-time wallet login challenge scoped to a wallet, site and network.
 */
@TableName("wallet_login_challenge")
public record WalletLoginChallengeDO() {
}

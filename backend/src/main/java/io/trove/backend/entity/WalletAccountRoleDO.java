package io.trove.backend.entity;

import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 钱包身份与后端特殊权限的关联，普通用户权限由登录身份获得。
 * Wallet-to-backend-role grants. Regular user capabilities come from the authenticated identity.
 *
 * <p>复合主键为 {@code (wallet_address, role_code)}。
 * The composite primary key is {@code (wallet_address, role_code)}.
 */
@TableName("wallet_account_role")
public record WalletAccountRoleDO() {
}

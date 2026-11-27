package io.trove.backend.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.OffsetDateTime;

/**
 * 钱包身份与后端特殊权限的关联，普通用户权限由登录身份获得。
 * Wallet-to-backend-role grants. Regular user capabilities come from the authenticated identity.
 *
 * <p>复合主键为 {@code (wallet_address, role_code)}。
 * The composite primary key is {@code (wallet_address, role_code)}.
 *
 * @param walletAddress 获得权限的钱包公钥。
 *        Public key of the wallet receiving the role.
 * @param roleCode 首版特殊权限代码，WAREHOUSE_OPERATOR 为仓管员。
 *        Phase-one privileged role code, WAREHOUSE_OPERATOR for warehouse staff.
 * @param grantedByWallet 执行授权的人，空值表示由部署初始化。
 *        Wallet of the grantor, null for bootstrap grants.
 * @param grantedAt 权限授予时间，API 以 UTC 返回。
 *        Time the current grant was added, exposed in UTC by the API.
 */
@TableName("wallet_account_role")
public record WalletAccountRoleDO(
        @TableField(value = "wallet_address", updateStrategy = FieldStrategy.NEVER) String walletAddress,
        @TableField(value = "role_code", updateStrategy = FieldStrategy.NEVER) String roleCode,
        @TableField("granted_by_wallet") String grantedByWallet,
        @TableField("granted_at") OffsetDateTime grantedAt
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

    /**
     * 供 MyBatis-Plus Lambda 查询读取 {@code role_code} 主键列。
     * Exposes the primary-key column {@code role_code} for MyBatis-Plus Lambda queries.
     *
     * @return 主键列的当前值。 The current primary-key value.
     */
    public String getRoleCode() {
        return roleCode;
    }
}

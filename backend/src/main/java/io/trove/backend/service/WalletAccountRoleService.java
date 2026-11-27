package io.trove.backend.service;

import com.baomidou.mybatisplus.spring.service.IService;
import io.trove.backend.entity.WalletAccountRoleDO;

import java.util.List;

/**
 * 声明钱包角色授权的基础持久化服务契约。
 * Declares the base persistence service contract for wallet role grants.
 */
public interface WalletAccountRoleService extends IService<WalletAccountRoleDO> {

    /** 仓库工作台及仓库写操作共用的角色代码。 / Role code for the workbench and warehouse mutations. */
    String WAREHOUSE_OPERATOR = "WAREHOUSE_OPERATOR";

    /**
     * 获取已认证钱包当前的数据库角色。 / Fetch current database roles for an authenticated wallet.
     *
     * @param walletAddress 会话中已经认证的 Solana 公钥 / authenticated Solana public key from the session
     * @return 已授权的角色代码，普通钱包为空列表 / granted role codes, empty for regular wallets
     */
    List<String> rolesFor(String walletAddress);
}

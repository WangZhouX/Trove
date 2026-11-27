package io.trove.backend.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import io.trove.backend.entity.WalletAccountRoleDO;
import io.trove.backend.mapper.WalletAccountRoleMapper;
import io.trove.backend.service.WalletAccountRoleService;

/**
 * 将钱包角色授权的基础服务与 {@link WalletAccountRoleMapper} 关联。
 * Connects the base service for wallet role grants to {@link WalletAccountRoleMapper}.
 */
public class WalletAccountRoleServiceImpl extends ServiceImpl<WalletAccountRoleMapper, WalletAccountRoleDO>
        implements WalletAccountRoleService {
}

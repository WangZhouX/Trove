package io.trove.backend.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import io.trove.backend.entity.WalletAccountRoleDO;
import io.trove.backend.mapper.WalletAccountRoleMapper;
import io.trove.backend.service.WalletAccountRoleService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 将钱包角色授权的基础服务与 {@link WalletAccountRoleMapper} 关联。
 * Connects the base service for wallet role grants to {@link WalletAccountRoleMapper}.
 */
@Service
public class WalletAccountRoleServiceImpl extends ServiceImpl<WalletAccountRoleMapper, WalletAccountRoleDO>
        implements WalletAccountRoleService {

    /** {@inheritDoc} */
    @Override
    public List<String> rolesFor(String walletAddress) {
        // 每次受保护的请求查询角色，授权撤销后下一次请求立即生效。
        // Read roles on each protected request so revocations take effect on the next request.
        return list(Wrappers.lambdaQuery(WalletAccountRoleDO.class)
                .eq(WalletAccountRoleDO::getWalletAddress, walletAddress)
                .orderByAsc(WalletAccountRoleDO::getRoleCode))
                .stream().map(WalletAccountRoleDO::roleCode).toList();
    }
}

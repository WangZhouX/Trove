package io.trove.backend.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import io.trove.backend.entity.WalletAccountDO;
import io.trove.backend.mapper.WalletAccountMapper;
import io.trove.backend.service.WalletAccountService;

/**
 * 将钱包身份的基础服务与 {@link WalletAccountMapper} 关联。
 * Connects the base service for wallet identities to {@link WalletAccountMapper}.
 */
public class WalletAccountServiceImpl extends ServiceImpl<WalletAccountMapper, WalletAccountDO>
        implements WalletAccountService {
}

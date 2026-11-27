package io.trove.backend.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import io.trove.backend.entity.WalletLoginChallengeDO;
import io.trove.backend.mapper.WalletLoginChallengeMapper;
import io.trove.backend.service.WalletLoginChallengeService;

/**
 * 将一次性钱包登录挑战的基础服务与 {@link WalletLoginChallengeMapper} 关联。
 * Connects the base service for one-time wallet login challenges to {@link WalletLoginChallengeMapper}.
 */
public class WalletLoginChallengeServiceImpl extends ServiceImpl<WalletLoginChallengeMapper, WalletLoginChallengeDO>
        implements WalletLoginChallengeService {
}

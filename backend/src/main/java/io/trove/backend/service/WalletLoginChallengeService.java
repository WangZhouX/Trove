package io.trove.backend.service;

import com.baomidou.mybatisplus.spring.service.IService;
import io.trove.backend.entity.WalletLoginChallengeDO;

/**
 * 声明一次性钱包登录挑战的基础持久化服务契约。
 * Declares the base persistence service contract for one-time wallet login challenges.
 */
public interface WalletLoginChallengeService extends IService<WalletLoginChallengeDO> {
}

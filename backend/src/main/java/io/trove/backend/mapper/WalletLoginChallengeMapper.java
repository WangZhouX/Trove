package io.trove.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.trove.backend.entity.WalletLoginChallengeDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 为 {@code wallet_login_challenge} 表中的一次性钱包登录挑战声明 MyBatis-Plus 映射接口。
 * Declares the MyBatis-Plus mapper for one-time wallet login challenges in {@code wallet_login_challenge}.
 */
@Mapper
public interface WalletLoginChallengeMapper extends BaseMapper<WalletLoginChallengeDO> {
}

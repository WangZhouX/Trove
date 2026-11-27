package io.trove.backend.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import io.trove.backend.entity.RedemptionDraftDO;
import io.trove.backend.mapper.RedemptionDraftMapper;
import io.trove.backend.service.RedemptionDraftService;

/**
 * 将私有兑换草稿的基础服务与 {@link RedemptionDraftMapper} 关联。
 * Connects the base service for private redemption drafts to {@link RedemptionDraftMapper}.
 */
public class RedemptionDraftServiceImpl extends ServiceImpl<RedemptionDraftMapper, RedemptionDraftDO>
        implements RedemptionDraftService {
}

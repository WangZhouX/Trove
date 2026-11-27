package io.trove.backend.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import io.trove.backend.entity.ChainEventDO;
import io.trove.backend.mapper.ChainEventMapper;
import io.trove.backend.service.ChainEventService;

/**
 * 将已最终确认的链上事件的基础服务与 {@link ChainEventMapper} 关联。
 * Connects the base service for finalized on-chain events to {@link ChainEventMapper}.
 */
public class ChainEventServiceImpl extends ServiceImpl<ChainEventMapper, ChainEventDO>
        implements ChainEventService {
}

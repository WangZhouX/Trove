package io.trove.backend.service;

import com.baomidou.mybatisplus.spring.service.IService;
import io.trove.backend.entity.ChainEventDO;

/**
 * 声明已最终确认的链上事件的基础持久化服务契约。
 * Declares the base persistence service contract for finalized on-chain events.
 */
public interface ChainEventService extends IService<ChainEventDO> {
}

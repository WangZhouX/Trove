package io.trove.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.trove.backend.entity.ChainEventDO;

/**
 * 为 {@code chain_event} 表中的已最终确认的链上事件声明 MyBatis-Plus 映射接口。
 * Declares the MyBatis-Plus mapper for finalized on-chain events in {@code chain_event}.
 */
public interface ChainEventMapper extends BaseMapper<ChainEventDO> {
}

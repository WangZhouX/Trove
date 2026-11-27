package io.trove.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.trove.backend.entity.ShipmentEventDO;

/**
 * 为 {@code shipment_event} 表中的发货任务履约时间线声明 MyBatis-Plus 映射接口。
 * Declares the MyBatis-Plus mapper for shipment fulfillment timelines in {@code shipment_event}.
 */
public interface ShipmentEventMapper extends BaseMapper<ShipmentEventDO> {
}

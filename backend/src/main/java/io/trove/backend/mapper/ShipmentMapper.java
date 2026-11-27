package io.trove.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.trove.backend.entity.ShipmentDO;

/**
 * 为 {@code shipment} 表中的兑换发货任务与履约状态声明 MyBatis-Plus 映射接口。
 * Declares the MyBatis-Plus mapper for redemption shipment tasks and fulfillment status in {@code shipment}.
 */
public interface ShipmentMapper extends BaseMapper<ShipmentDO> {
}

package io.trove.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.trove.backend.entity.ShipmentEventDO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.apache.ibatis.annotations.Mapper;

import java.util.UUID;

/**
 * 为 {@code shipment_event} 表中的发货任务履约时间线声明 MyBatis-Plus 映射接口。
 * Declares the MyBatis-Plus mapper for shipment fulfillment timelines in {@code shipment_event}.
 */
@Mapper
public interface ShipmentEventMapper extends BaseMapper<ShipmentEventDO> {

    /**
     * 使用任务编号与幂等键共同查询登记结果。调用方已检查任务网络和角色。
     * Look up a recorded action by both task ID and idempotency key after the caller checks network and role.
     *
     * @param shipmentId 已授权的任务编号 / authorized task identifier
     * @param eventKey 同任务内的重试键 / retry key scoped to the shipment
     * @return 首次登记的事件，未登记时为空 / original event, or null if not recorded
     */
    default ShipmentEventDO selectByKey(UUID shipmentId, String eventKey) {
        return selectOne(Wrappers.lambdaQuery(ShipmentEventDO.class)
                .eq(ShipmentEventDO::getShipmentId, shipmentId)
                .eq(ShipmentEventDO::getEventKey, eventKey));
    }
}

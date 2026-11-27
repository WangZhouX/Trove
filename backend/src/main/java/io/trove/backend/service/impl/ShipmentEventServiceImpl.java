package io.trove.backend.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import io.trove.backend.entity.ShipmentEventDO;
import io.trove.backend.mapper.ShipmentEventMapper;
import io.trove.backend.service.ShipmentEventService;

/**
 * 将发货任务履约时间线的基础服务与 {@link ShipmentEventMapper} 关联。
 * Connects the base service for shipment fulfillment timelines to {@link ShipmentEventMapper}.
 */
public class ShipmentEventServiceImpl extends ServiceImpl<ShipmentEventMapper, ShipmentEventDO>
        implements ShipmentEventService {
}

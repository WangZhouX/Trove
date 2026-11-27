package io.trove.backend.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import io.trove.backend.entity.ShipmentDO;
import io.trove.backend.mapper.ShipmentMapper;
import io.trove.backend.service.ShipmentService;

/**
 * 将兑换发货任务与履约状态的基础服务与 {@link ShipmentMapper} 关联。
 * Connects the base service for redemption shipment tasks and fulfillment status to {@link ShipmentMapper}.
 */
public class ShipmentServiceImpl extends ServiceImpl<ShipmentMapper, ShipmentDO>
        implements ShipmentService {
}

package io.trove.backend.service;

import com.baomidou.mybatisplus.spring.service.IService;
import io.trove.backend.entity.ShipmentDO;

/**
 * 声明兑换发货任务与履约状态的基础持久化服务契约。
 * Declares the base persistence service contract for redemption shipment tasks and fulfillment status.
 */
public interface ShipmentService extends IService<ShipmentDO> {
}

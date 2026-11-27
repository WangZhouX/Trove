package io.trove.backend.service;

import com.baomidou.mybatisplus.spring.service.IService;
import io.trove.backend.entity.ShipmentEventDO;

/**
 * 声明发货任务履约时间线的基础持久化服务契约。
 * Declares the base persistence service contract for shipment fulfillment timelines.
 */
public interface ShipmentEventService extends IService<ShipmentEventDO> {
}

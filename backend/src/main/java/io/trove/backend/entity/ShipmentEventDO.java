package io.trove.backend.entity;

import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 单笔发货任务的私有履约时间线，按事件键去重。
 * Private fulfillment timeline for a shipment, deduplicated by event key.
 *
 * <p>复合主键为 {@code (shipment_id, event_key)}。
 * The composite primary key is {@code (shipment_id, event_key)}.
 */
@TableName("shipment_event")
public record ShipmentEventDO() {
}

package io.trove.backend.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 单笔发货任务的私有履约时间线，按事件键去重。
 * Private fulfillment timeline for a shipment, deduplicated by event key.
 *
 * <p>复合主键为 {@code (shipment_id, event_key)}。
 * The composite primary key is {@code (shipment_id, event_key)}.
 *
 * @param shipmentId 发货任务 UUID，对应当前履约状态。
 *        Shipment task UUID linking to the current fulfillment state.
 * @param eventKey 同任务内跨重试稳定的事件幂等键。
 *        Idempotency key that remains stable across retries for the same shipment.
 * @param eventType 履约事件类型，含任务创建、发出、签收、异常或异常关闭。
 *        Fulfillment event type covering task creation, shipping, delivery, exception, or exception
 *        resolution.
 * @param actorWallet 操作仓管员的钱包公钥，自动记录的事件可为空。
 *        Public wallet address of the warehouse operator, nullable for automatic events.
 * @param reasonCode 异常的非敏感原因代码，普通事件为空。
 *        Non-sensitive exception reason code, nullable for ordinary events.
 * @param occurredAt 履约事件发生时间。
 *        Time when the fulfillment event occurred.
 */
@TableName("shipment_event")
public record ShipmentEventDO(
        @TableField(value = "shipment_id", updateStrategy = FieldStrategy.NEVER) UUID shipmentId,
        @TableField(value = "event_key", updateStrategy = FieldStrategy.NEVER) String eventKey,
        @TableField("event_type") String eventType,
        @TableField("actor_wallet") String actorWallet,
        @TableField("reason_code") String reasonCode,
        @TableField("occurred_at") OffsetDateTime occurredAt
) {

    /**
     * 供 MyBatis-Plus Lambda 查询读取 {@code shipment_id} 主键列。
     * Exposes the primary-key column {@code shipment_id} for MyBatis-Plus Lambda queries.
     *
     * @return 主键列的当前值。 The current primary-key value.
     */
    public UUID getShipmentId() {
        return shipmentId;
    }

    /**
     * 供 MyBatis-Plus Lambda 查询读取 {@code event_key} 主键列。
     * Exposes the primary-key column {@code event_key} for MyBatis-Plus Lambda queries.
     *
     * @return 主键列的当前值。 The current primary-key value.
     */
    public String getEventKey() {
        return eventKey;
    }
}

package io.trove.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 已最终确认兑换的私有发货任务，收件资料通过凭证回查草稿。
 * Private shipment task for a finalized redemption, with recipient details resolved through the receipt and draft.
 *
 * @param shipmentId 服务端生成的发货任务 UUID。
 *        Server-generated UUID of the shipment task.
 * @param network 关联兑换凭证的 Solana 网络。
 *        Solana network of the linked redemption receipt.
 * @param itemId 关联兑换凭证的实体藏品 UUID。
 *        Physical collectible UUID of the linked redemption receipt.
 * @param status 履约当前状态，含待发货、已发货、已签收和异常。
 *        Current fulfillment status covering ready, shipped, delivered, and exception.
 * @param carrierCode 承运商代码，模拟发货可使用演示代码，未登记时为空。
 *        Carrier code, including a demo code for simulated shipping, nullable before registration.
 * @param trackingNumber 私有物流运单号，未登记时为空。
 *        Private shipment tracking number, nullable before registration.
 * @param shippedAt 仓库登记实物交付物流的时间，发出前为空。
 *        Time the warehouse recorded release to the carrier, nullable before shipment.
 * @param deliveredAt 登记签收的时间，签收前为空。
 *        Recorded delivery timestamp, nullable before delivery.
 * @param createdAt 发货任务创建时间。
 *        Shipment task creation timestamp.
 * @param updatedAt 应用最后更新发货任务的时间。
 *        Application timestamp of the latest shipment task update.
 */
@TableName("shipment")
public record ShipmentDO(
        @TableId(value = "shipment_id", type = IdType.INPUT) UUID shipmentId,
        @TableField("network") String network,
        @TableField("item_id") UUID itemId,
        @TableField("status") String status,
        @TableField("carrier_code") String carrierCode,
        @TableField("tracking_number") String trackingNumber,
        @TableField("shipped_at") OffsetDateTime shippedAt,
        @TableField("delivered_at") OffsetDateTime deliveredAt,
        @TableField("created_at") OffsetDateTime createdAt,
        @TableField("updated_at") OffsetDateTime updatedAt
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
}

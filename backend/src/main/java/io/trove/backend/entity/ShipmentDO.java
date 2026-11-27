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

    /** @return 发货任务关联的链上网络 / chain network of the shipment task */
    public String getNetwork() {
        return network;
    }
    /** @return 绑定的实物编号 / bound physical item identifier */
    public UUID getItemId() {
        return itemId;
    }
    /** @return 履约任务当前状态 / current fulfillment status */
    public String getStatus() {
        return status;
    }
    /** @return 登记的承运商代码，未发货时为空 / recorded carrier code, null before dispatch */
    public String getCarrierCode() {
        return carrierCode;
    }
    /** @return 登记的私有运单号，未发货时为空 / recorded private tracking number, null before dispatch */
    public String getTrackingNumber() {
        return trackingNumber;
    }
    /** @return 首次出库时间，未出库时为空 / initial dispatch time, null before dispatch */
    public OffsetDateTime getShippedAt() {
        return shippedAt;
    }
    /** @return 签收时间，未签收时为空 / delivery time, null before delivery */
    public OffsetDateTime getDeliveredAt() {
        return deliveredAt;
    }
    /** @return 任务创建时间 / task creation time */
    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
    /** @return 任务更新时间，用于防止覆盖并发修改 / task update time used to prevent concurrent overwrites */
    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

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

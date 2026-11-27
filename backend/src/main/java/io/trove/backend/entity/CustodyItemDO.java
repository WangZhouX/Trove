package io.trove.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 验收入库后的实物身份、仓位与出库事实。
 * Accepted physical item identity, storage location and outbound status.
 *
 * @param itemId 实物稳定 UUID，对应链上 itemId，验收后复用及保留。
 *        Stable physical-item UUID corresponding to on-chain itemId and retained after acceptance.
 * @param consignmentId 对应的一笔已验收寄存单，每个寄存单最多生成一件在库实物。
 *        Related accepted consignment, with at most one custody item per consignment.
 * @param physicalIdentity 核验后的实物唯一键，与寄存单的规范化键一致并永久保留。
 *        Verified physical identity key matching the normalized consignment key and retained after checkout.
 * @param displayTitle 验真后允许公开的藏品标题。
 *        Collectible title approved for public display after verification.
 * @param verifiedCondition 仓管员核验后的品相。
 *        Physical condition verified by warehouse staff.
 * @param publicImageUri 经仓库审核且移除私人资料的公开封面 URI，可空表示尚无公开图。
 *        Approved public cover URI with private content removed, null when no public image is available.
 * @param warehouseLocation 库管使用的实物仓位编码，出库后保留最后仓位。
 *        Internal warehouse location code, retaining the last location after checkout.
 * @param state 实物状态，IN_VAULT 为在库，SHIPPED_OUT 为已出库。
 *        Physical status, IN_VAULT for stored items and SHIPPED_OUT after warehouse release.
 * @param createdAt 实物验收并建立入库记录的时间。
 *        Time the item was accepted and its custody record created.
 * @param shippedOutAt 仓库登记实物出库的时间，空值表示仍在库。
 *        Recorded warehouse release time, null while in storage.
 * @param updatedAt 后端最近一次更新在库记录的时间。
 *        Most recent custody update time maintained by the backend.
 */
@TableName("custody_item")
public record CustodyItemDO(
        @TableId(value = "item_id", type = IdType.INPUT) UUID itemId,
        @TableField("consignment_id") UUID consignmentId,
        @TableField("physical_identity") String physicalIdentity,
        @TableField("display_title") String displayTitle,
        @TableField("verified_condition") String verifiedCondition,
        @TableField("public_image_uri") String publicImageUri,
        @TableField("warehouse_location") String warehouseLocation,
        @TableField("state") String state,
        @TableField("created_at") OffsetDateTime createdAt,
        @TableField("shipped_out_at") OffsetDateTime shippedOutAt,
        @TableField("updated_at") OffsetDateTime updatedAt
) {

    /** @return 实物保管状态 / physical custody state */
    public String getState() {
        return state;
    }
    /** @return 仓库登记的首次出库时间，仍在库时为空 / initial dispatch time recorded by the warehouse, null while in vault */
    public OffsetDateTime getShippedOutAt() {
        return shippedOutAt;
    }
    /** @return 在库记录最后更新时间 / latest update time on the custody record */
    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * 供 MyBatis-Plus Lambda 查询读取 {@code item_id} 主键列。
     * Exposes the primary-key column {@code item_id} for MyBatis-Plus Lambda queries.
     *
     * @return 主键列的当前值。 The current primary-key value.
     */
    public UUID getItemId() {
        return itemId;
    }

    /** @return 寄存单 UUID，用于定位单据关联的在库记录。 / Consignment UUID for custody lookup. */
    public UUID getConsignmentId() {
        return consignmentId;
    }

    /** @return 实物唯一键，用于查验历史入库记录。 / Physical identity key for prior custody checks. */
    public String getPhysicalIdentity() {
        return physicalIdentity;
    }
}

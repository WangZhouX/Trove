package io.trove.backend.entity;

import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 已最终确认兑换的私有发货任务，收件资料通过凭证回查草稿。
 * Private shipment task for a finalized redemption, with recipient details resolved through the receipt and draft.
 */
@TableName("shipment")
public record ShipmentDO() {
}

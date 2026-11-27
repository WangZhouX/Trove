package io.trove.backend.entity;

import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 已最终确认的链上兑换凭证投影，也是创建发货任务的依据。
 * Projection of a finalized on-chain redemption receipt and prerequisite for shipment creation.
 *
 * <p>复合主键为 {@code (network, item_id)}。
 * The composite primary key is {@code (network, item_id)}.
 */
@TableName("redemption_receipt")
public record RedemptionReceiptDO() {
}

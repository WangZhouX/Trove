package io.trove.backend.entity;

import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 持有人私有兑换草稿及加密收件资料，进入签名后冻结资料。
 * Holder-private redemption draft and encrypted shipping details, frozen after entering signing.
 */
@TableName("redemption_draft")
public record RedemptionDraftDO() {
}

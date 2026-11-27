package io.trove.backend.entity;

import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 验收入库后的实物身份、仓位与出库事实。
 * Accepted physical item identity, storage location and outbound status.
 */
@TableName("custody_item")
public record CustodyItemDO() {
}

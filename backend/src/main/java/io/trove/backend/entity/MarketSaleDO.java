package io.trove.backend.entity;

import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 经最终确认的 Trove 成交记录，每笔挂单最多对应一次成交。
 * Finalized Trove sales, with at most one sale for each listing.
 *
 * <p>复合主键为 {@code (network, item_id, listing_no)}。
 * The composite primary key is {@code (network, item_id, listing_no)}.
 */
@TableName("market_sale")
public record MarketSaleDO() {
}

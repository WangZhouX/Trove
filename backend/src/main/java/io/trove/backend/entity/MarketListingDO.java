package io.trove.backend.entity;

import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 链上固定价托管挂单的查询投影，更新报价会推进同一挂单的版本。
 * Query projection of fixed-price on-chain escrow listings, with repricing advancing the version of the same listing.
 *
 * <p>复合主键为 {@code (network, item_id, listing_no)}。
 * The composite primary key is {@code (network, item_id, listing_no)}.
 */
@TableName("market_listing")
public record MarketListingDO() {
}

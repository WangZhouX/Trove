package io.trove.backend.entity;

import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 最终确认后形成的 Trove 实物与 NFT 的链上绑定投影，烧毁 NFT 后仍保留。
 * Finalized projection of a Trove physical item NFT binding, retained after the NFT is burned.
 *
 * <p>复合主键为 {@code (network, item_id)}。
 * The composite primary key is {@code (network, item_id)}.
 */
@TableName("chain_asset")
public record ChainAssetDO() {
}

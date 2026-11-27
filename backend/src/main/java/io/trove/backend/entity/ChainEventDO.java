package io.trove.backend.entity;

import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 经 RPC 核验且已达到最终确认的 Trove 成功交易事件，作为索引去重凭证。
 * Verified events from successful, finalized Trove transactions, used to deduplicate indexing.
 *
 * <p>复合主键为 {@code (network, tx_signature, event_index)}。
 * The composite primary key is {@code (network, tx_signature, event_index)}.
 */
@TableName("chain_event")
public record ChainEventDO() {
}

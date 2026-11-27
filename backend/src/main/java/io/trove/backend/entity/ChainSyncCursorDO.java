package io.trove.backend.entity;

import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 按网络和程序分开的最终确认交易连续扫描进度。
 * Per-network and per-program checkpoint of the contiguous finalized transaction scan.
 *
 * <p>复合主键为 {@code (network, program_id)}。
 * The composite primary key is {@code (network, program_id)}.
 */
@TableName("chain_sync_cursor")
public record ChainSyncCursorDO() {
}

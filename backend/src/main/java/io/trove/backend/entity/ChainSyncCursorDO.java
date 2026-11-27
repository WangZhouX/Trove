package io.trove.backend.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigInteger;
import java.time.OffsetDateTime;

/**
 * 按网络和程序分开的最终确认交易连续扫描进度。
 * Per-network and per-program checkpoint of the contiguous finalized transaction scan.
 *
 * <p>复合主键为 {@code (network, program_id)}。
 * The composite primary key is {@code (network, program_id)}.
 *
 * @param network 扫描任务对应的 Solana 网络。
 *        Solana network scanned by this task.
 * @param programId 扫描交易时使用的 Trove 程序公钥。
 *        Trove program address used in transaction scans.
 * @param lastTxSignature 最后一笔已连续处理的交易签名，尚未建立扫描进度时为空。
 *        Last transaction signature processed in contiguous scan order, null before a checkpoint exists.
 * @param lastSlot 扫描断点交易所在区块槽，无断点时为空。
 *        Slot of the checkpoint transaction, null before a checkpoint exists.
 * @param updatedAt 最近更新扫描进度的时间。
 *        Time the scan checkpoint was last updated.
 */
@TableName("chain_sync_cursor")
public record ChainSyncCursorDO(
        @TableField(value = "network", updateStrategy = FieldStrategy.NEVER) String network,
        @TableField(value = "program_id", updateStrategy = FieldStrategy.NEVER) String programId,
        @TableField("last_tx_signature") String lastTxSignature,
        @TableField("last_slot") BigInteger lastSlot,
        @TableField("updated_at") OffsetDateTime updatedAt
) {

    /**
     * 供 MyBatis-Plus Lambda 查询读取 {@code network} 主键列。
     * Exposes the primary-key column {@code network} for MyBatis-Plus Lambda queries.
     *
     * @return 主键列的当前值。 The current primary-key value.
     */
    public String getNetwork() {
        return network;
    }

    /**
     * 供 MyBatis-Plus Lambda 查询读取 {@code program_id} 主键列。
     * Exposes the primary-key column {@code program_id} for MyBatis-Plus Lambda queries.
     *
     * @return 主键列的当前值。 The current primary-key value.
     */
    public String getProgramId() {
        return programId;
    }
}

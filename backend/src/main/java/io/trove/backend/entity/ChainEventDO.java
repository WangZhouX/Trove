package io.trove.backend.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.trove.backend.entity.typehandler.JsonbStringTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.math.BigInteger;
import java.time.OffsetDateTime;

/**
 * 经 RPC 核验且已达到最终确认的 Trove 成功交易事件，作为索引去重凭证。
 * Verified events from successful, finalized Trove transactions, used to deduplicate indexing.
 *
 * <p>复合主键为 {@code (network, tx_signature, event_index)}。
 * The composite primary key is {@code (network, tx_signature, event_index)}.
 *
 * @param network 事件所属 Solana 网络。
 *        Solana network of the event.
 * @param txSignature 链上交易的 Base58 签名。
 *        Base58-encoded on-chain transaction signature.
 * @param eventIndex 交易内从 0 开始的稳定事件序号。
 *        Stable zero-based event ordinal within the transaction.
 * @param programId 发出事件的 Trove 程序公钥。
 *        Address of the Trove program that emitted the event.
 * @param eventType 服务端按 Anchor IDL 解码后的事件类型。
 *        Event type decoded by the service using the Anchor IDL.
 * @param slot RPC 返回的区块槽编号。
 *        Slot number reported by RPC.
 * @param blockTime RPC 提供的链上时间，未返回时为空。
 *        On-chain time from RPC, null when unavailable.
 * @param payload 仅包含已解码且允许保存的公开业务字段。
 *        Allowlisted public business fields decoded from the event.
 * @param indexedAt 事件首次写入数据库的时间。
 *        Time the event was first recorded in the database.
 */
@TableName("chain_event")
public record ChainEventDO(
        @TableField(value = "network", updateStrategy = FieldStrategy.NEVER) String network,
        @TableField(value = "tx_signature", updateStrategy = FieldStrategy.NEVER) String txSignature,
        @TableField(value = "event_index", updateStrategy = FieldStrategy.NEVER) int eventIndex,
        @TableField("program_id") String programId,
        @TableField("event_type") String eventType,
        @TableField("slot") BigInteger slot,
        @TableField("block_time") OffsetDateTime blockTime,
        @TableField(value = "payload", jdbcType = JdbcType.OTHER, typeHandler = JsonbStringTypeHandler.class) String payload,
        @TableField("indexed_at") OffsetDateTime indexedAt
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
     * 供 MyBatis-Plus Lambda 查询读取 {@code tx_signature} 主键列。
     * Exposes the primary-key column {@code tx_signature} for MyBatis-Plus Lambda queries.
     *
     * @return 主键列的当前值。 The current primary-key value.
     */
    public String getTxSignature() {
        return txSignature;
    }

    /**
     * 供 MyBatis-Plus Lambda 查询读取 {@code event_index} 主键列。
     * Exposes the primary-key column {@code event_index} for MyBatis-Plus Lambda queries.
     *
     * @return 主键列的当前值。 The current primary-key value.
     */
    public int getEventIndex() {
        return eventIndex;
    }
}

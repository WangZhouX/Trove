package io.trove.backend.api;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 链上交易回执的公开字段，供已签名钱包查询最终确认的索引结果。
 * Defines the public receipt fields used by signed-in wallets to read finalized indexed results.
 */
public final class ChainTransactionApi {

    /**
     * 仅定义接口数据类型。 / Holds API data types only.
     */
    private ChainTransactionApi() {
    }

    /**
     * 本地索引已经登记并核验的交易回执。
     * A transaction receipt recorded and verified by the local index.
     *
     * @param network 签名会话绑定的 Solana 网络 / Solana network bound to the authenticated session
     * @param signature 64 字节 Solana 交易签名的 Base58 编码 / Base58 encoding of the 64-byte Solana transaction signature
     * @param status 回执已达到最终确认并完成本地索引 / receipt is finalized and locally indexed
     * @param events 当前钱包可查看的事件摘要，按事件序号升序 / event summaries visible to the wallet, ordered by event index
     */
    public record Receipt(String network, String signature, Status status, List<EventSummary> events) {
    }

    /**
     * 当前支持的回执状态来自持久化的最终确认事件。
     * The supported receipt status comes from persisted finalized events.
     */
    public enum Status {
        FINALIZED_INDEXED
    }

    /**
     * 允许关联钱包查看的最终确认事件摘要。
     * A finalized event summary available to an associated wallet.
     *
     * @param eventIndex 交易内从 0 开始的事件序号 / zero-based event ordinal within the transaction
     * @param programId 产生事件的 Solana 程序地址 / Solana program address that emitted the event
     * @param eventType 索引器确认的 Trove 事件类型 / Trove event type recognized by the indexer
     * @param slot 区块槽的无符号十进制字符串 / unsigned decimal string of the slot number
     * @param blockTime 区块时间，RPC 未提供时为空 / block time, null when unavailable from RPC
     * @param indexedAt 后端首次索引成功时间 / time the backend first indexed this event
     */
    public record EventSummary(
            int eventIndex,
            String programId,
            String eventType,
            String slot,
            OffsetDateTime blockTime,
            OffsetDateTime indexedAt
    ) {
    }

    /**
     * 交易回执查询失败时的对外错误。
     * Public error returned when a receipt lookup cannot complete.
     *
     * @param code 稳定的错误码 / stable error code
     * @param message 页面可显示的提示 / feedback suitable for the client
     */
    public record ErrorResponse(String code, String message) {
    }
}

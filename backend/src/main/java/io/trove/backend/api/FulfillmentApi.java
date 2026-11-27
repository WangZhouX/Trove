package io.trove.backend.api;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** 兑换进度与仓库履约的 HTTP 类型。 / HTTP types for redemption progress and warehouse fulfillment. */
public final class FulfillmentApi {

    private FulfillmentApi() {
    }

    /**
     * 已授权查询的分页结果。 / Page of results from an authorized query.
     *
     * @param <T> 当前接口的记录类型 / row type for the endpoint
     * @param items 当前页记录 / current page rows
     * @param page 从 1 开始的页码 / one-based page number
     * @param size 每页条数 / requested page size
     * @param total 命中条件的记录总数 / total rows matching the filters
     */
    public record PageResult<T>(List<T> items, int page, int size, long total) {
    }

    /** 履约任务状态。 / Fulfillment task status. */
    public enum ShipmentStatus {
        READY, SHIPPED, DELIVERED, EXCEPTION
    }

    /** 库管员可登记的事件类型。 / Events that a warehouse operator can record. */
    public enum EventType {
        SHIPPED, DELIVERED, EXCEPTION, EXCEPTION_RESOLVED
    }

    /**
     * 兑换人的私有进度摘要。 / Private progress summary for the redeemer.
     *
     * @param draftId 草稿 UUID / draft UUID
     * @param itemId 实物 UUID / physical item UUID
     * @param draftStatus 当前草稿状态 / current draft state
     * @param createdAt 草稿创建时间 / draft creation time
     * @param updatedAt 草稿更新时间 / draft update time
     * @param receiptSignature 已最终确认的兑换签名，未确认时为空 / finalized redemption signature, null before confirmation
     * @param shipment 发货进度，未生成任务时为空 / shipping progress, null until the task exists
     */
    public record RedemptionProgress(UUID draftId, UUID itemId, String draftStatus, OffsetDateTime createdAt,
                                     OffsetDateTime updatedAt, String receiptSignature, ShipmentProgress shipment) {
    }

    /**
     * 只向经授权的兑换人和库管员返回的任务进度。 / Task progress visible to the authorized redeemer and warehouse operators.
     *
     * @param shipmentId 发货任务 UUID / shipment task UUID
     * @param itemId 实物 UUID / physical item UUID
     * @param status 当前任务状态 / current task status
     * @param carrierCode 承运商代码，未发货时为空 / carrier code, null before shipping
     * @param trackingNumber 私有运单号，未发货时为空 / private tracking number, null before shipping
     * @param createdAt 任务创建时间 / task creation time
     * @param shippedAt 首次出库时间，未发货时为空 / initial dispatch time, null before shipping
     * @param deliveredAt 签收时间，未签收时为空 / delivery time, null before delivery
     * @param updatedAt 进度最近更新时间 / most recent progress update time
     */
    public record ShipmentProgress(UUID shipmentId, UUID itemId, ShipmentStatus status, String carrierCode,
                                   String trackingNumber, OffsetDateTime createdAt, OffsetDateTime shippedAt,
                                   OffsetDateTime deliveredAt, OffsetDateTime updatedAt) {
    }

    /**
     * 库管员提交的一次履约操作。 / One fulfillment operation submitted by an operator.
     *
     * @param eventKey 同一任务中跨重试保持不变的 ASCII 幂等键，最多 128 字符 / stable ASCII retry key scoped to the task, up to 128 characters
     * @param type 事件类型 / event type
     * @param carrierCode 发货时必填的承运商代码 / carrier code required for dispatch
     * @param trackingNumber 发货时必填的运单号 / tracking number required for dispatch
     * @param reasonCode 仅异常时提供的非敏感原因代码 / non-sensitive reason code supplied for exceptions only
     */
    public record RecordEventRequest(String eventKey, EventType type, String carrierCode, String trackingNumber,
                                     String reasonCode) {
    }

    /**
     * 登记履约事件后的任务状态。 / Task status after recording a fulfillment event.
     *
     * @param shipment 任务当前状态，重复请求也返回最新进度 / task state, including latest progress on replays
     * @param eventKey 已持久化的幂等键 / recorded idempotency key
     * @param eventType 已记录的事件类型 / recorded event type
     * @param occurredAt 事件首次登记时间 / time the event was first recorded
     */
    public record EventResult(ShipmentProgress shipment, String eventKey, EventType eventType, OffsetDateTime occurredAt) {
    }
}

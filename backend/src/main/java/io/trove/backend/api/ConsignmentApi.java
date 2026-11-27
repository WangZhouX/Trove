package io.trove.backend.api;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 寄存人和库管员访问寄存业务的 HTTP 请求与响应。
 * HTTP request and response types for depositor and warehouse consignment actions.
 */
public final class ConsignmentApi {

    private ConsignmentApi() {
    }

    /**
     * 寄存人提交的实物与寄件信息。所属钱包由当前会话提供。
     * Item and inbound parcel details from the depositor, whose wallet comes from the session.
     *
     * @param clientRequestId 同一钱包内的重试 UUID / retry UUID scoped to the current wallet
     * @param category 藏品类别，CARD 或 TOY / collectible category, CARD or TOY
     * @param submittedTitle 申报名称 / title declared by the depositor
     * @param submittedCondition 申报品相 / condition declared by the depositor
     * @param identityKind 编号种类，GRADING_CERTIFICATE 或 ITEM_SERIAL / identifier type, GRADING_CERTIFICATE or ITEM_SERIAL
     * @param identityIssuer 鉴定机构或厂商 / grading service or manufacturer
     * @param identityNumber 证书编号或实物序列号 / certificate number or item serial
     * @param inboundCarrier 可选寄入承运商 / optional inbound carrier
     * @param inboundTrackingNumber 可选寄入运单号 / optional inbound tracking number
     */
    public record CreateRequest(UUID clientRequestId, String category, String submittedTitle,
                                String submittedCondition, String identityKind, String identityIssuer,
                                String identityNumber, String inboundCarrier, String inboundTrackingNumber) {
    }

    /**
     * 库管员验真通过后记录的实物信息。 / Item facts recorded by warehouse staff after a successful inspection.
     *
     * @param displayTitle 已核验并可公开展示的标题 / verified title suitable for public display
     * @param verifiedCondition 现场核验的品相 / condition verified during inspection
     * @param warehouseLocation 内部仓位编码 / internal storage location code
     */
    public record ApproveRequest(String displayTitle, String verifiedCondition, String warehouseLocation) {
    }

    /**
     * 库管员验真驳回时填写的说明。 / Explanation entered by warehouse staff when rejecting an item.
     *
     * @param rejectionReason 寄存人可查看的拒收原因 / rejection reason visible to the depositor
     */
    public record RejectRequest(String rejectionReason) {
    }

    /**
     * 拒收实物寄回后的登记信息。 / Details recorded after a rejected item has been returned.
     *
     * @param returnTrackingNumber 已知时填写退件运单号 / return tracking number when available
     */
    public record ReturnRequest(String returnTrackingNumber) {
    }

    /**
     * 用于控制器区分首次受理和幂等重试的服务结果。
     * Service result distinguishing an initial submission from an idempotent replay.
     *
     * @param consignment 当前寄存单 / current consignment
     * @param created 本次请求首次创建时为 true / true when this request created the consignment
     */
    public record Submission(Detail consignment, boolean created) {
    }

    /**
     * 「我的寄存」或「入库与验真」使用的分页数据。
     * Page of results for My Consignments or the warehouse Intake and Inspection queue.
     *
     * @param items 当前页的寄存单 / consignments on the current page
     * @param page 从 1 开始的页码 / one-based page number
     * @param size 请求的每页条数 / requested page size
     * @param total 符合当前条件的寄存单总数 / total number matching the current filters
     */
    public record PageResult(List<Detail> items, int page, int size, long total) {
    }

    /**
     * 已通过数据归属或库管权限检查的寄存申请与进度。
     * Consignment details and progress after depositor ownership or warehouse access is verified.
     *
     * @param consignmentId 寄存单 UUID / consignment UUID
     * @param depositorWallet 原寄存人的钱包公钥 / original depositor's wallet public key
     * @param category 藏品类别 / collectible category
     * @param submittedTitle 寄存人申报的名称 / title declared by the depositor
     * @param submittedCondition 寄存人申报的品相 / condition declared by the depositor
     * @param identityKind 实物编号种类 / physical identifier type
     * @param identityIssuer 实物编号签发方 / issuer of the physical identifier
     * @param identityNumber 实物证书编号或序列号 / physical certificate number or serial
     * @param inboundCarrier 可选寄入承运商 / optional inbound carrier
     * @param inboundTrackingNumber 可选寄入运单号 / optional inbound tracking number
     * @param status 当前业务状态代码 / current workflow status code
     * @param receivedAt 登记收货的 UTC 时间，未收货时为空 / UTC time of receipt, null before receipt
     * @param reviewedAt 记录验收决定的 UTC 时间，未验收时为空 / UTC time of inspection, null before review
     * @param rejectionReason 拒收原因，未拒收时为空 / rejection reason, null until rejection
     * @param returnedAt 实物退回的 UTC 时间，未退回时为空 / UTC time of return, null before return
     * @param returnTrackingNumber 可选退件运单号 / optional return tracking number
     * @param createdAt 申请创建的 UTC 时间 / UTC time of submission
     * @param updatedAt 最后更新的 UTC 时间 / UTC time of the latest update
     * @param itemId 验收产生的稳定实物 UUID，未建档时为空 / stable accepted-item UUID, null before custody registration
     */
    public record Detail(UUID consignmentId, String depositorWallet, String category, String submittedTitle,
                         String submittedCondition, String identityKind, String identityIssuer, String identityNumber,
                         String inboundCarrier, String inboundTrackingNumber, String status, OffsetDateTime receivedAt,
                         OffsetDateTime reviewedAt, String rejectionReason, OffsetDateTime returnedAt,
                         String returnTrackingNumber, OffsetDateTime createdAt, OffsetDateTime updatedAt, UUID itemId) {
    }
}

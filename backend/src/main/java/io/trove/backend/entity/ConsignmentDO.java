package io.trove.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 钱包寄存申报、收货验收、铸造结果及拒收退件记录。
 * Wallet consignment submissions, receiving and inspection, minting results and rejection returns.
 *
 * @param consignmentId 后端生成的 UUID 寄存单编号。
 *        Server-generated UUID consignment identifier.
 * @param depositorWallet 原寄存人钱包，验收通过后 NFT 铸造到此地址。
 *        Original depositor wallet and recipient of the NFT after approval.
 * @param clientRequestId 客户端提供的重试标识，同一钱包内唯一。
 *        Client-provided idempotency identifier, unique per wallet.
 * @param category 首版藏品类型，CARD 为卡牌，TOY 为潮玩。
 *        Phase-one category, CARD for cards and TOY for art toys.
 * @param submittedTitle 寄存人提交的名称，仓管验真后另存已核验标题。
 *        Name supplied by the depositor, with the verified title recorded separately after inspection.
 * @param submittedCondition 寄存人申报的品相或描述。
 *        Condition or description reported by the depositor.
 * @param identityKind 实物凭据类型，鉴定证书或实物序列号。
 *        Type of physical identifier, grading certificate or item serial.
 * @param identityIssuer 鉴定机构或制造商等编号命名空间的来源。
 *        Source of the identifier namespace, such as the grading service or manufacturer.
 * @param identityNumber 寄存人申报的证书编号或序列号原文。
 *        Original certificate number or serial as supplied by the depositor.
 * @param physicalIdentity 后端按凭据类型、机构命名空间和机构规则规范化后的实物唯一键。
 *        Backend-normalized physical identity key combining kind, issuer namespace and the issuer-normalized
 *        identifier.
 * @param inboundCarrier 寄入承运商，空值表示未登记。
 *        Inbound carrier, null when unrecorded.
 * @param inboundTrackingNumber 寄入运单号，空值表示未登记。
 *        Inbound tracking number, null when unrecorded.
 * @param status 寄存业务状态，依次登记到仓、验收、待铸造或拒收退件。
 *        Consignment workflow state covering receipt, review, minting or rejected return.
 * @param receivedAt 仓库登记到货时间，空值表示未收货。
 *        Warehouse receiving time, null before receipt.
 * @param receivedByWallet 登记收货的仓管钱包，空值表示未收货。
 *        Warehouse operator wallet that recorded receipt, null before receipt.
 * @param reviewedAt 仓管签发验收决定的时间，空值表示未决定。
 *        Time warehouse staff recorded a review decision, null before decision.
 * @param reviewedByWallet 签发验收决定的仓管钱包，空值表示未决定。
 *        Warehouse operator wallet recording the review, null before decision.
 * @param rejectionReason 拒收原因，接受或未决定时为空。
 *        Reject reason, null until rejection or for accepted consignments.
 * @param returnedAt 仓管办理拒收退件的时间，空值表示尚未退件或无需退件。
 *        Time the rejected item was returned, null while pending or not applicable.
 * @param returnTrackingNumber 退件运单号，空值表示未登记或不适用。
 *        Return tracking number, null when unrecorded or inapplicable.
 * @param createdAt 寄存单创建时间，API 以 UTC 返回。
 *        Consignment creation time, exposed in UTC by the API.
 * @param updatedAt 后端最近一次更新寄存单的时间。
 *        Most recent consignment update time maintained by the backend.
 */
@TableName("consignment")
public record ConsignmentDO(
        @TableId(value = "consignment_id", type = IdType.INPUT) UUID consignmentId,
        @TableField("depositor_wallet") String depositorWallet,
        @TableField("client_request_id") UUID clientRequestId,
        @TableField("category") String category,
        @TableField("submitted_title") String submittedTitle,
        @TableField("submitted_condition") String submittedCondition,
        @TableField("identity_kind") String identityKind,
        @TableField("identity_issuer") String identityIssuer,
        @TableField("identity_number") String identityNumber,
        @TableField("physical_identity") String physicalIdentity,
        @TableField("inbound_carrier") String inboundCarrier,
        @TableField("inbound_tracking_number") String inboundTrackingNumber,
        @TableField("status") String status,
        @TableField("received_at") OffsetDateTime receivedAt,
        @TableField("received_by_wallet") String receivedByWallet,
        @TableField("reviewed_at") OffsetDateTime reviewedAt,
        @TableField("reviewed_by_wallet") String reviewedByWallet,
        @TableField("rejection_reason") String rejectionReason,
        @TableField("returned_at") OffsetDateTime returnedAt,
        @TableField("return_tracking_number") String returnTrackingNumber,
        @TableField("created_at") OffsetDateTime createdAt,
        @TableField("updated_at") OffsetDateTime updatedAt
) {

    /**
     * 供 MyBatis-Plus Lambda 查询读取 {@code consignment_id} 主键列。
     * Exposes the primary-key column {@code consignment_id} for MyBatis-Plus Lambda queries.
     *
     * @return 主键列的当前值。 The current primary-key value.
     */
    public UUID getConsignmentId() {
        return consignmentId;
    }
}

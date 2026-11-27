import { apiFetch } from "../../../shared/http/apiFetch";
import type { Consignment, ConsignmentCategory, ConsignmentIdentityKind } from "../consignmentTypes";

/** 寄存人申报的藏品和寄件信息。 / Collectible and shipping information declared by the depositor. */
export interface CreateConsignmentRequest {
  /** 客户端生成的 UUID，同一钱包的相同申请重试时复用。 / Client-generated UUID reused when the same wallet retries the same application. */
  clientRequestId: string;
  /** 申报的藏品类别。 / Category declared by the depositor. */
  category: ConsignmentCategory;
  /** 申报名称，最多 120 字。 / Declared title, up to 120 characters. */
  submittedTitle: string;
  /** 申报品相，最多 120 字。 / Declared condition, up to 120 characters. */
  submittedCondition: string;
  /** 实物编号的凭据类型。 / Type of the evidence bearing the physical identifier. */
  identityKind: ConsignmentIdentityKind;
  /** 鉴定机构或厂商，最多 100 字。 / Grading service or manufacturer, up to 100 characters. */
  identityIssuer: string;
  /** 证书编号或序列号，最多 120 字。 / Certificate number or serial, up to 120 characters. */
  identityNumber: string;
  /** 可选寄入承运商，最多 80 字。 / Optional inbound carrier, up to 80 characters. */
  inboundCarrier?: string;
  /** 可选寄入运单号，最多 120 字。 / Optional inbound tracking number, up to 120 characters. */
  inboundTrackingNumber?: string;
}

/** 首次提交和相同请求重试共用的单据响应。 / Consignment returned for both creation and an unchanged retry. */
export type CreateConsignmentResponse = Consignment;

/**
 * 用当前会话提交寄存申请，寄存人由服务端从会话确定。 / Submits a consignment under the depositor identified by the server session.
 * @param token 当前钱包的会话令牌。 / Current wallet's session token.
 * @param request 藏品资料、可选寄件资料和重试 UUID。 / Collectible details, optional shipping details, and retry UUID.
 * @param signal 用于取消请求的信号。 / Signal that can cancel the request.
 * @returns 新建或已存在的寄存单。 / New or existing consignment record.
 * @throws 会话失效、入参无效或实物和请求编号冲突时返回对应的 HTTP 错误。 / Session expiration, invalid input, or identity and request conflicts surface as HTTP errors.
 */
export function createConsignment(
  token: string,
  request: CreateConsignmentRequest,
  signal?: AbortSignal,
): Promise<CreateConsignmentResponse> {
  return apiFetch("/api/v1/consignments", { method: "POST", token, body: request, signal });
}

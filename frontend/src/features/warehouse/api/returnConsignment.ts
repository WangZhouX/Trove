import { apiFetch } from "../../../shared/http/apiFetch";
import type { Consignment } from "../../consignments/consignmentTypes";

/** 可提交的处理资料。 / Workflow details to submit. */
export interface ReturnConsignmentRequest {
  /** 可选退件运单号，最多 120 字。 / Optional return tracking number, up to 120 characters. */
  returnTrackingNumber?: string;
}

/** 服务端核验后的单据详情。 / Consignment detail after backend verification. */
export type ReturnConsignmentResponse = Consignment;

/**
 * 库管员登记已拒收实物的退回。 / Records the return of a rejected consignment.
 * @param token 已授权库管员的会话令牌。 / Authorized warehouse operator's session token.
 * @param id 寄存单 UUID。 / Consignment UUID.
 * @param request 可提交的处理资料。 / Workflow details to submit.
 * @param signal 用于取消请求的信号。 / Signal that can cancel the request.
 * @returns 更新后的寄存单及状态。 / Updated consignment and workflow state.
 */
export function returnConsignment(
  token: string,
  id: Consignment["consignmentId"],
  request: ReturnConsignmentRequest,
  signal?: AbortSignal,
): Promise<ReturnConsignmentResponse> {
  return apiFetch(`/api/v1/warehouse/consignments/${encodeURIComponent(id)}/return`, {
    method: "POST", token, body: request, signal,
  });
}

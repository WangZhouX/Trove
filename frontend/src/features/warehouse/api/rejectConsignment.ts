import { apiFetch } from "../../../shared/http/apiFetch";
import type { Consignment } from "../../consignments/consignmentTypes";

/** 可提交的处理资料。 / Workflow details to submit. */
export interface RejectConsignmentRequest {
  /** 寄存人可见的拒收原因，最多 500 字。 / Reason shown to the depositor, up to 500 characters. */
  rejectionReason: string;
}

/** 服务端核验后的单据详情。 / Consignment detail after backend verification. */
export type RejectConsignmentResponse = Consignment;

/**
 * 库管员拒收已收货实物并登记原因。 / Rejects a received consignment and records the reason.
 * @param token 已授权库管员的会话令牌。 / Authorized warehouse operator's session token.
 * @param id 寄存单 UUID。 / Consignment UUID.
 * @param request 可提交的处理资料。 / Workflow details to submit.
 * @param signal 用于取消请求的信号。 / Signal that can cancel the request.
 * @returns 更新后的寄存单及状态。 / Updated consignment and workflow state.
 */
export function rejectConsignment(
  token: string,
  id: Consignment["consignmentId"],
  request: RejectConsignmentRequest,
  signal?: AbortSignal,
): Promise<RejectConsignmentResponse> {
  return apiFetch(`/api/v1/warehouse/consignments/${encodeURIComponent(id)}/reject`, {
    method: "POST", token, body: request, signal,
  });
}

import { apiFetch } from "../../../shared/http/apiFetch";
import type { Consignment } from "../../consignments/consignmentTypes";

/** 可提交的处理资料。 / Workflow details to submit. */
export interface ApproveConsignmentRequest {
  /** 验收通过后可展示的藏品名称，最多 120 字。 / Verified title suitable for display, up to 120 characters. */
  displayTitle: string;
  /** 核验品相，最多 120 字。 / Verified condition, up to 120 characters. */
  verifiedCondition: string;
  /** 内部仓位编码，最多 100 字。 / Internal warehouse location code, up to 100 characters. */
  warehouseLocation: string;
}

/** 服务端核验后的单据详情。 / Consignment detail after backend verification. */
export type ApproveConsignmentResponse = Consignment;

/**
 * 库管员验收通过实物并登记仓位。 / Approves an inspected item and records its warehouse location.
 * @param token 已授权库管员的会话令牌。 / Authorized warehouse operator's session token.
 * @param id 寄存单 UUID。 / Consignment UUID.
 * @param request 可提交的处理资料。 / Workflow details to submit.
 * @param signal 用于取消请求的信号。 / Signal that can cancel the request.
 * @returns 更新后的寄存单及状态。 / Updated consignment and workflow state.
 */
export function approveConsignment(
  token: string,
  id: Consignment["consignmentId"],
  request: ApproveConsignmentRequest,
  signal?: AbortSignal,
): Promise<ApproveConsignmentResponse> {
  return apiFetch(`/api/v1/warehouse/consignments/${encodeURIComponent(id)}/approve`, {
    method: "POST", token, body: request, signal,
  });
}

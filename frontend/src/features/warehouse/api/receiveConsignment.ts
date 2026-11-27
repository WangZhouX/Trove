import { apiFetch } from "../../../shared/http/apiFetch";
import type { Consignment } from "../../consignments/consignmentTypes";

/** 单据的请求 UUID。 / Consignment UUID used in the request. */
export type ReceiveConsignmentRequest = Consignment["consignmentId"];

/** 服务端核验后的单据详情。 / Consignment detail after backend verification. */
export type ReceiveConsignmentResponse = Consignment;

/**
 * 库管员确认待寄入实物已收货。 / Records receipt of a consignment awaiting arrival.
 * @param token 已授权库管员的会话令牌。 / Authorized warehouse operator's session token.
 * @param id 寄存单 UUID。 / Consignment UUID.
 * @param signal 用于取消请求的信号。 / Signal that can cancel the request.
 * @returns 更新后的寄存单及状态。 / Updated consignment and workflow state.
 */
export function receiveConsignment(
  token: string,
  id: ReceiveConsignmentRequest,
  signal?: AbortSignal,
): Promise<ReceiveConsignmentResponse> {
  return apiFetch(`/api/v1/warehouse/consignments/${encodeURIComponent(id)}/receive`, {
    method: "POST", token, signal,
  });
}

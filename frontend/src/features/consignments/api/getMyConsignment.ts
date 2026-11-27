import { apiFetch } from "../../../shared/http/apiFetch";
import type { Consignment } from "../consignmentTypes";

/** 单据的请求 UUID。 / Consignment UUID used in the request. */
export type GetMyConsignmentRequest = Consignment["consignmentId"];

/** 服务端核验后的单据详情。 / Consignment detail after backend verification. */
export type GetMyConsignmentResponse = Consignment;

/**
 * 查询当前钱包的一笔寄存。 / Reads a consignment owned by the current wallet.
 * @param token 当前钱包的会话令牌。 / Current wallet's session token.
 * @param id 寄存单 UUID。 / Consignment UUID.
 * @param signal 用于取消请求的信号。 / Signal that can cancel the request.
 * @returns 当前寄存单及状态。 / Current consignment and workflow state.
 */
export function getMyConsignment(
  token: string,
  id: GetMyConsignmentRequest,
  signal?: AbortSignal,
): Promise<GetMyConsignmentResponse> {
  return apiFetch(`/api/v1/consignments/${encodeURIComponent(id)}`, {
    token, signal,
  });
}

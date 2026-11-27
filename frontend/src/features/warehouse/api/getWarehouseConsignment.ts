import { apiFetch } from "../../../shared/http/apiFetch";
import type { Consignment } from "../../consignments/consignmentTypes";

/** 单据的请求 UUID。 / Consignment UUID used in the request. */
export type GetWarehouseConsignmentRequest = Consignment["consignmentId"];

/** 服务端核验后的单据详情。 / Consignment detail after backend verification. */
export type GetWarehouseConsignmentResponse = Consignment;

/**
 * 供库管员查询一笔寄存详情。 / Reads a consignment detail for warehouse staff.
 * @param token 已授权库管员的会话令牌。 / Authorized warehouse operator's session token.
 * @param id 寄存单 UUID。 / Consignment UUID.
 * @param signal 用于取消请求的信号。 / Signal that can cancel the request.
 * @returns 当前寄存单及状态。 / Current consignment and workflow state.
 */
export function getWarehouseConsignment(
  token: string,
  id: GetWarehouseConsignmentRequest,
  signal?: AbortSignal,
): Promise<GetWarehouseConsignmentResponse> {
  return apiFetch(`/api/v1/warehouse/consignments/${encodeURIComponent(id)}`, {
    token, signal,
  });
}

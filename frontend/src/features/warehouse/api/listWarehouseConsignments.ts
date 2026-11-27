import { apiFetch } from "../../../shared/http/apiFetch";
import { consignmentListQuery } from "../../consignments/api/consignmentListQuery";
import type { ConsignmentListParams, ConsignmentPage } from "../../consignments/consignmentTypes";

/** 仓库待办列表参数。 / Warehouse queue parameters. */
export type ListWarehouseConsignmentsRequest = ConsignmentListParams;

/** 仓库待办列表分页。 / Page of warehouse queue entries. */
export type ListWarehouseConsignmentsResponse = ConsignmentPage;

/**
 * 查询按创建时间正序排列的仓库列表。 / Lists warehouse consignments in ascending creation order.
 * @param token 已授权库管员的会话令牌。 / Authorized warehouse operator's session token.
 * @param params 状态筛选及分页条件。 / Status filter and pagination parameters.
 * @param signal 用于取消请求的信号。 / Signal that can cancel the request.
 * @returns 当前页单据与匹配总数。 / Current page of consignments and total number of matches.
 */
export function listWarehouseConsignments(
  token: string,
  params: ListWarehouseConsignmentsRequest,
  signal?: AbortSignal,
): Promise<ListWarehouseConsignmentsResponse> {
  return apiFetch(`/api/v1/warehouse/consignments${consignmentListQuery(params)}`, { token, signal });
}

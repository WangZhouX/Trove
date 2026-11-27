import { apiFetch } from "../../../shared/http/apiFetch";
import { consignmentListQuery } from "./consignmentListQuery";
import type { ConsignmentListParams, ConsignmentPage } from "../consignmentTypes";

/** 本人寄存列表参数。 / Parameters for the depositor's consignment list. */
export type ListMyConsignmentsRequest = ConsignmentListParams;

/** 本人寄存列表分页。 / Page of the depositor's consignments. */
export type ListMyConsignmentsResponse = ConsignmentPage;

/**
 * 查询当前钱包的寄存记录，最新申请排在前面。 / Lists the current wallet's consignments, newest first.
 * @param token 当前钱包的会话令牌。 / Current wallet's session token.
 * @param params 状态筛选及分页条件。 / Status filter and pagination parameters.
 * @param signal 用于取消请求的信号。 / Signal that can cancel the request.
 * @returns 当前页单据与匹配总数。 / Current page of consignments and total number of matches.
 */
export function listMyConsignments(
  token: string,
  params: ListMyConsignmentsRequest,
  signal?: AbortSignal,
): Promise<ListMyConsignmentsResponse> {
  return apiFetch(`/api/v1/consignments/me${consignmentListQuery(params)}`, { token, signal });
}

import type { ConsignmentListParams } from "../consignmentTypes";

/**
 * 构造两个列表接口共用的可选查询字段。 / Builds the optional query fields shared by both list endpoints.
 * @param params 状态、页码和每页条数。 / Status, page number, and entries per page.
 * @returns 带问号的查询字符串，无查询字段时为空字符串。 / Query string with its question mark, or an empty string when no fields are supplied.
 */
export function consignmentListQuery(params: ConsignmentListParams): string {
  const query = new URLSearchParams();
  if (params.status !== undefined) query.set("status", params.status);
  if (params.page !== undefined) query.set("page", String(params.page));
  if (params.size !== undefined) query.set("size", String(params.size));
  const encoded = query.toString();
  return encoded ? `?${encoded}` : "";
}

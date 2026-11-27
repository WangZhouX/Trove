import { skipToken, useQuery, useQueryClient } from "@tanstack/react-query";
import { useEffect, useState } from "react";
import { getMyConsignment } from "../../features/consignments/api/getMyConsignment";
import { listMyConsignments } from "../../features/consignments/api/listMyConsignments";
import {
  consignmentCategoryLabels,
  consignmentIdentityKindLabels,
  consignmentStatusLabels,
} from "../../features/consignments/consignmentTypes";
import type { Consignment, ConsignmentStatus } from "../../features/consignments/consignmentTypes";
import { useWalletAuth } from "../../features/wallet-auth/WalletAuthProvider";
import { ApiError } from "../../shared/http/apiFetch";
import { ConsignForm } from "./ConsignForm";
import "./styles/consign.css";

/** 分页大小和本地时间格式。 / Page size and localized timestamp formatting. */
const pageSize = 10;
const dateTime = new Intl.DateTimeFormat("zh-CN", { dateStyle: "medium", timeStyle: "short" });

/** 寄存页面只在当前钱包已签名时加载私有资料。 / The consignment page loads private records after wallet sign-in. */
export function ConsignPage() {
  const { session } = useWalletAuth();
  // 使用钱包、网络和会话到期时间区分表单与列表，旧会话离开时直接卸载。
  // Scope the form and list by wallet, network, and session expiry so that an old session unmounts.
  const sessionKey = session && [session.walletAddress, session.network, session.expiresAt].join(":");
  return (
    <section className="page consign-page">
      <h1>寄存</h1>
      <p>提交实物信息后，可以查看仓库的收货、验收和退件进度。</p>
      {session && sessionKey ? (
        <ConsignWorkspace key={sessionKey} token={session.token} sessionKey={sessionKey} />
      ) : (
        <p className="consign-notice">请先通过顶部的 Phantom 钱包入口签名登录。</p>
      )}
    </section>
  );
}

/**
 * 同一登录会话下管理申请、分页和当前展开的详情。
 * Manages submission, pagination, and the open detail within one signed-in session.
 * @param token 已签名的短期会话令牌。 / Signed short-lived session token.
 * @param sessionKey 钱包、网络和会话版本组合的页面作用域。 / Page scope for the wallet, network, and session version.
 */
function ConsignWorkspace({ token, sessionKey }: { token: string; sessionKey: string }) {
  const { expireSession } = useWalletAuth();
  const queryClient = useQueryClient();
  const [status, setStatus] = useState<ConsignmentStatus | "">("");
  const [page, setPage] = useState(1);
  const [selectedId, setSelectedId] = useState<string | null>(null);

  const listKey = ["my-consignment-list", sessionKey];
  const detailKey = ["my-consignment-detail", sessionKey];
  const list = useQuery({
    queryKey: [...listKey, status, page],
    queryFn: ({ signal }) => listMyConsignments(token, { status: status || undefined, page, size: pageSize }, signal),
    retry: false,
    gcTime: 0,
  });
  const detail = useQuery({
    queryKey: [...detailKey, selectedId],
    queryFn: selectedId === null ? skipToken : ({ signal }) => getMyConsignment(token, selectedId, signal),
    retry: false,
    gcTime: 0,
  });

  useEffect(() => {
    if ((list.error instanceof ApiError && list.error.status === 401)
      || (detail.error instanceof ApiError && detail.error.status === 401)) {
      expireSession(token);
    }
  }, [detail.error, expireSession, list.error, token]);

  /**
   * 申请成功后回到未筛选的第一页，展开后端返回的单据。
   * After submission, reset to the unfiltered first page and open the returned record.
   * @param consignment 已保存的寄存单。 / Persisted consignment.
   */
  function onCreated(consignment: Consignment) {
    void queryClient.invalidateQueries({ queryKey: listKey });
    setStatus("");
    setPage(1);
    setSelectedId(consignment.consignmentId);
  }

  const total = list.data?.total ?? 0;
  const pageCount = Math.max(1, Math.ceil(total / pageSize));
  return (
    <div className="consign-layout">
      <ConsignForm token={token} onCreated={onCreated} />
      <section className="consign-panel" aria-labelledby="my-consignments-title">
        <div className="consign-section-heading">
          <h2 id="my-consignments-title">我的寄存</h2>
          <button className="site-wallet__button site-wallet__button--secondary" type="button"
            disabled={list.isFetching} onClick={() => {
              void list.refetch();
              if (selectedId) void detail.refetch();
            }}>
            刷新
          </button>
        </div>
        <label className="consign-field">
          <span>寄存状态</span>
          <select value={status} onChange={(event) => {
            setStatus(event.target.value as ConsignmentStatus | "");
            setPage(1);
            setSelectedId(null);
          }}>
            <option value="">全部状态</option>
            {Object.entries(consignmentStatusLabels).map(([value, label]) =>
              <option key={value} value={value}>{label}</option>)}
          </select>
        </label>
        {list.isPending ? <p role="status">正在加载寄存申请…</p> : null}
        {list.isError ? <p role="alert" className="consign-error">{queryError(list.error, "寄存列表加载失败。")}</p> : null}
        {list.data && !list.isError && total === 0 ? <p>这个状态下暂无寄存申请。</p> : null}
        {list.data && !list.isError && total > 0 ? (
          <>
            <ul className="consign-list">
              {list.data.items.map((row) => (
                <li key={row.consignmentId} className="consign-row">
                  <div>
                    <strong>{row.submittedTitle}</strong>
                    <p>{consignmentCategoryLabels[row.category]} · {formatTime(row.createdAt)}</p>
                  </div>
                  <span className="consign-status">{consignmentStatusLabels[row.status]}</span>
                  <button type="button" className="consign-link-button" aria-expanded={selectedId === row.consignmentId}
                    onClick={() => setSelectedId(selectedId === row.consignmentId ? null : row.consignmentId)}>
                    {selectedId === row.consignmentId ? "收起详情" : "查看详情"}
                  </button>
                </li>
              ))}
            </ul>
            <div className="consign-pagination">
              <button className="site-wallet__button site-wallet__button--secondary" type="button"
                disabled={page <= 1 || list.isFetching} onClick={() => {
                  setPage((value) => value - 1);
                  setSelectedId(null);
                }}>上一页</button>
              <p>第 {page} / {pageCount} 页 · 共 {total} 条</p>
              <button className="site-wallet__button site-wallet__button--secondary" type="button"
                disabled={page >= pageCount || list.isFetching} onClick={() => {
                  setPage((value) => value + 1);
                  setSelectedId(null);
                }}>下一页</button>
            </div>
          </>
        ) : null}
        {selectedId && detail.isPending ? <p role="status">正在加载寄存详情…</p> : null}
        {selectedId && detail.isError ? (
          <p role="alert" className="consign-error">{queryError(detail.error, "寄存详情加载失败。")}</p>
        ) : null}
        {selectedId && detail.data && !detail.isError ? <ConsignmentDetail record={detail.data} /> : null}
      </section>
    </div>
  );
}

/**
 * 展示当前钱包有权查看的寄存资料、进度和验收生成的实物编号。
 * Shows the depositor's consignment data, progress, and the item ID assigned after approval.
 * @param record 已由后端校验归属的寄存详情。 / Consignment detail verified by the backend.
 */
function ConsignmentDetail({ record }: { record: Consignment }) {
  const inbound = [record.inboundCarrier, record.inboundTrackingNumber].filter(Boolean).join(" · ");
  return (
    <section className="consign-detail" aria-label="寄存申请详情">
      <div className="consign-section-heading">
        <h3>{record.submittedTitle}</h3>
        <span className="consign-status">{consignmentStatusLabels[record.status]}</span>
      </div>
      <dl>
        <dt>寄存单号</dt><dd><code>{record.consignmentId}</code></dd>
        <dt>品相描述</dt><dd>{record.submittedCondition}</dd>
        <dt>{consignmentIdentityKindLabels[record.identityKind]}</dt>
        <dd>{record.identityIssuer} · {record.identityNumber}</dd>
        <dt>提交时间</dt><dd>{formatTime(record.createdAt)}</dd>
        {inbound ? <><dt>寄入物流</dt><dd>{inbound}</dd></> : null}
        {record.receivedAt ? <><dt>收货时间</dt><dd>{formatTime(record.receivedAt)}</dd></> : null}
        {record.reviewedAt ? <><dt>验收时间</dt><dd>{formatTime(record.reviewedAt)}</dd></> : null}
        {record.rejectionReason ? <><dt>驳回原因</dt><dd>{record.rejectionReason}</dd></> : null}
        {record.returnedAt ? <><dt>退回时间</dt><dd>{formatTime(record.returnedAt)}</dd></> : null}
        {record.returnTrackingNumber ? <><dt>退件运单号</dt><dd>{record.returnTrackingNumber}</dd></> : null}
        {record.itemId ? <><dt>实物编号</dt><dd><code>{record.itemId}</code></dd></> : null}
      </dl>
    </section>
  );
}

/**
 * 将 API 的时间字符串转换为本地显示。 / Format the API timestamp for local display.
 * @param value 后端返回的 ISO 时间。 / ISO timestamp returned by the backend.
 * @returns 本地时间文字。 / Local timestamp text.
 */
function formatTime(value: string): string {
  return dateTime.format(new Date(value));
}

/**
 * 显示后端业务提示或页面自己的简明错误。 / Show a business response or the page's fallback error.
 * @param error 查询错误。 / Query error.
 * @param fallback 网络错误的显示文字。 / Message for a network failure.
 * @returns 页面提示。 / User-facing feedback.
 */
function queryError(error: unknown, fallback: string): string {
  return error instanceof ApiError ? error.message : fallback;
}

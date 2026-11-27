import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useEffect, useRef, useState } from "react";
import { consignmentCategoryLabels, consignmentStatusLabels } from "../../features/consignments/consignmentTypes";
import type { Consignment, ConsignmentStatus } from "../../features/consignments/consignmentTypes";
import { getWarehouseConsignment } from "../../features/warehouse/api/getWarehouseConsignment";
import { listWarehouseConsignments } from "../../features/warehouse/api/listWarehouseConsignments";
import { ApiError } from "../../shared/http/apiFetch";
import { WarehouseConsignmentDetail } from "./WarehouseConsignmentDetail";

/** 单据队列每页行数。 / Rows per warehouse queue page. */
const pageSize = 10;

/** 使用当前会话提交一项仓库操作，返回服务端更新后的单据。 / Submit an operation with the active session and return the updated consignment. */
export type WarehouseAction = (signal: AbortSignal) => Promise<Consignment>;

interface WarehouseWorkbenchProps {
  /** 已验证库管会话的令牌，仅用于发起请求。 / Verified operator session token, used only in API calls. */
  token: string;
  /** 钱包、网络和会话有效期组成的缓存范围。 / Cache scope made up of wallet, network, and session expiration. */
  scope: readonly string[];
  /** 处理当前会话的 401 或 403，返回是否已处理。 / Handle a 401 or 403 from this session and report whether it was handled. */
  onAuthError(error: unknown): boolean;
}

/**
 * 展示当前库管会话的队列、详情和操作结果。 / Show the current operator's queue, selected record, and action results.
 * @param props 已验证会话的令牌、缓存范围和授权错误处理。 / Verified token, cache scope and authorization error handler.
 * @returns 显示寄存流程的工作台。 / The workbench for the consignment workflow.
 */
export function WarehouseWorkbench({ token, scope, onAuthError }: WarehouseWorkbenchProps) {
  const queryClient = useQueryClient();
  // 筛选、分页和详情选择仅保存在当前工作台实例中。 / Keep status, pagination, and selection within the active workbench instance.
  const [status, setStatus] = useState<ConsignmentStatus | "">("");
  const [page, setPage] = useState(1);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [feedback, setFeedback] = useState<{ message: string; error: boolean } | null>(null);
  const inFlight = useRef<AbortController | null>(null);

  // 不把令牌写入缓存键，队列与详情共享账户范围。 / Keep tokens out of cache keys and share an account scope for queue and detail.
  const scopeKey = ["warehouse-consignments", ...scope] as const;
  const queue = useQuery({
    queryKey: [...scopeKey, "list", page, status],
    queryFn: ({ signal }) => listWarehouseConsignments(token, { page, size: pageSize, status: status || undefined }, signal),
    retry: false,
    gcTime: 0,
  });
  const detail = useQuery({
    queryKey: [...scopeKey, "detail", selectedId],
    queryFn: ({ signal }) => getWarehouseConsignment(token, selectedId!, signal),
    enabled: selectedId !== null,
    retry: false,
    gcTime: 0,
  });

  useEffect(() => {
    if (queue.error && onAuthError(queue.error)) return;
    if (detail.error) onAuthError(detail.error);
  }, [queue.error, detail.error, onAuthError]);

  // 切换账户或离开工作台时取消仍在等待的操作响应。 / Cancel pending action responses on account switch or departure.
  useEffect(() => () => inFlight.current?.abort(), []);

  /**
   * 串行提交当前单据的操作，从服务器刷新队列和详情。 / Serialize actions on the current selection and refresh both views from the server.
   * @param action 基于当前账户的 API 操作。 / API operation bound to the active account.
   */
  async function applyAction(action: WarehouseAction) {
    if (inFlight.current) return;
    const controller = new AbortController();
    inFlight.current = controller;
    setBusy(true);
    setFeedback(null);
    try {
      const updated = await action(controller.signal);
      if (controller.signal.aborted) return;
      queryClient.setQueryData([...scopeKey, "detail", updated.consignmentId], updated);
      setFeedback({ message: "寄存单已更新。", error: false });
      void queryClient.invalidateQueries({ queryKey: scopeKey });
    } catch (error) {
      if (controller.signal.aborted || onAuthError(error)) return;
      setFeedback({ message: error instanceof ApiError ? error.message : "操作失败，请检查连接后重试。", error: true });
      // 状态冲突时重新读取后端记录，仅保留当前可执行的操作。 / Reload after a state conflict so controls reflect the actual workflow state.
      if (error instanceof ApiError && error.status === 409) {
        void queryClient.invalidateQueries({ queryKey: scopeKey });
      }
    } finally {
      inFlight.current = null;
      if (!controller.signal.aborted) setBusy(false);
    }
  }

  return (
    <div className="warehouse-workbench">
      <section className="warehouse-queue" aria-labelledby="warehouse-queue-title">
        <h2 id="warehouse-queue-title">入库与验真</h2>
        <div className="warehouse-toolbar">
          <label className="warehouse-filter">
            按状态筛选
            <select value={status} disabled={busy} onChange={(event) => {
              setStatus(event.currentTarget.value as ConsignmentStatus | "");
              setPage(1);
            }}>
              <option value="">全部状态</option>
              {Object.entries(consignmentStatusLabels).map(([code, label]) => <option key={code} value={code}>{label}</option>)}
            </select>
          </label>
          <button type="button" className="warehouse-button warehouse-button--secondary" disabled={busy || queue.isFetching}
            onClick={() => void queryClient.invalidateQueries({ queryKey: scopeKey })}>
            刷新队列
          </button>
        </div>

        {queue.isPending && <p role="status">正在加载寄存队列…</p>}
        {queue.isError && <p role="alert">{queue.error instanceof ApiError ? queue.error.message : "队列加载失败，请刷新重试。"}</p>}
        {queue.data && (
          <>
            {queue.data.items.length === 0 ? <p>该页没有寄存单。</p> : (
              <ul className="warehouse-list">
                {queue.data.items.map((row) => (
                  <li key={row.consignmentId}>
                    <button type="button" className="warehouse-row" disabled={busy} aria-pressed={selectedId === row.consignmentId}
                      onClick={() => {
                        setSelectedId(row.consignmentId);
                        setFeedback(null);
                      }}>
                      <span><strong>{row.submittedTitle}</strong><small>{consignmentCategoryLabels[row.category]} · {row.identityNumber}</small></span>
                      <span className="warehouse-status-label">{consignmentStatusLabels[row.status]}</span>
                    </button>
                  </li>
                ))}
              </ul>
            )}
            <div className="warehouse-pagination" aria-label="仓库队列分页">
              <button type="button" className="warehouse-button warehouse-button--secondary" disabled={busy || page <= 1 || queue.isFetching}
                onClick={() => setPage(page - 1)}>上一页</button>
              <span>第 {page} 页 · 共 {queue.data.total} 条</span>
              <button type="button" className="warehouse-button warehouse-button--secondary" disabled={busy || page * pageSize >= queue.data.total || queue.isFetching}
                onClick={() => setPage(page + 1)}>下一页</button>
            </div>
          </>
        )}
      </section>

      <section className="warehouse-detail" aria-labelledby="warehouse-detail-title">
        <h2 id="warehouse-detail-title">寄存单详情</h2>
        {!selectedId && <p>从队列选择寄存单，查看进度和可用操作。</p>}
        {selectedId && detail.isPending && <p role="status">正在加载寄存单…</p>}
        {detail.isError && (
          <div className="warehouse-detail-feedback">
            <p role="alert">{detail.error instanceof ApiError ? detail.error.message : "寄存单加载失败。"}</p>
            <button type="button" className="warehouse-button warehouse-button--secondary" onClick={() => void detail.refetch()}>
              重试加载详情
            </button>
          </div>
        )}
        {detail.data && !detail.isError && (
          <WarehouseConsignmentDetail key={`${detail.data.consignmentId}:${detail.data.status}`} consignment={detail.data}
            busy={busy} token={token} onAction={(action) => void applyAction(action)} />
        )}
        {feedback && <p className="warehouse-feedback" role={feedback.error ? "alert" : "status"}>{feedback.message}</p>}
      </section>
    </div>
  );
}

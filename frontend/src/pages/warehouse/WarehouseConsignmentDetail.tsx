import type { FormEvent } from "react";
import { consignmentCategoryLabels, consignmentIdentityKindLabels, consignmentStatusLabels } from "../../features/consignments/consignmentTypes";
import type { Consignment } from "../../features/consignments/consignmentTypes";
import { approveConsignment } from "../../features/warehouse/api/approveConsignment";
import { receiveConsignment } from "../../features/warehouse/api/receiveConsignment";
import { rejectConsignment } from "../../features/warehouse/api/rejectConsignment";
import { returnConsignment } from "../../features/warehouse/api/returnConsignment";
import type { WarehouseAction } from "./WarehouseWorkbench";

/** 以浏览器本地时区显示后端返回的 ISO 时间。 / Display server ISO timestamps in the browser's local time zone. */
const dateFormatter = new Intl.DateTimeFormat("zh-CN", { dateStyle: "medium", timeStyle: "short" });

interface WarehouseConsignmentDetailProps {
  /** 经权限检查后取得的寄存单详情。 / Consignment detail retrieved after authorization. */
  consignment: Consignment;
  /** 当前是否已提交一项操作。 / Whether an action is already underway. */
  busy: boolean;
  /** 仅供接口请求使用的库管会话令牌。 / Operator session token used only for API calls. */
  token: string;
  /** 提交与当前单据匹配的一项状态操作。 / Submit a transition for the selected consignment. */
  onAction(action: WarehouseAction): void;
}

/**
 * 读取表单中的文本输入。 / Read a text input from the submitted form.
 * @param data 表单输入。 / Submitted form values.
 * @param field 字段名称。 / Field name.
 * @returns 去除首尾空白的文本，空字段返回空串。 / Trimmed text, or an empty string for an empty field.
 */
function readField(data: FormData, field: string): string {
  return String(data.get(field) ?? "").trim();
}

/**
 * 展示单据资料和当前状态允许的仓库操作。 / Show consignment information and actions allowed by its current status.
 * @param props 单据、操作状态、库管会话和提交回调。 / Record, operation status, operator session, and submission callback.
 * @returns 带状态操作的寄存详情。 / Consignment detail with workflow actions.
 */
export function WarehouseConsignmentDetail({ consignment, busy, token, onAction }: WarehouseConsignmentDetailProps) {
  // 以服务端详情的单据编号和状态为准，切换单据或状态时外层会重建表单。 / Use the server's record ID and status. The parent resets forms when either changes.
  const { consignmentId, status, itemId } = consignment;

  /**
   * 提交验收信息，后端保存入库实物并返回稳定编号。 / Submit inspection information so the server records the custody item and returns its stable ID.
   * @param event 验收表单提交事件。 / Inspection form submission event.
   */
  function submitApproval(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const data = new FormData(event.currentTarget);
    const request = {
      displayTitle: readField(data, "displayTitle"),
      verifiedCondition: readField(data, "verifiedCondition"),
      warehouseLocation: readField(data, "warehouseLocation"),
    };
    onAction((signal) => approveConsignment(token, consignmentId, request, signal));
  }

  /**
   * 提交寄存人可见的驳回原因。 / Submit a rejection reason visible to the depositor.
   * @param event 驳回表单提交事件。 / Rejection form submission event.
   */
  function submitRejection(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const rejectionReason = readField(new FormData(event.currentTarget), "rejectionReason");
    onAction((signal) => rejectConsignment(token, consignmentId, { rejectionReason }, signal));
  }

  /**
   * 登记已退回的实物及可选运单号。 / Record the completed return and optional tracking number.
   * @param event 退回表单提交事件。 / Return form submission event.
   */
  function submitReturn(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const returnTrackingNumber = readField(new FormData(event.currentTarget), "returnTrackingNumber") || undefined;
    onAction((signal) => returnConsignment(token, consignmentId, { returnTrackingNumber }, signal));
  }

  return (
    <div className="warehouse-record" aria-busy={busy}>
      <div className="warehouse-record-heading">
        <h3>{consignment.submittedTitle}</h3>
        <span className="warehouse-status-label">{consignmentStatusLabels[status]}</span>
      </div>
      <dl className="warehouse-record-fields">
        <div><dt>单据编号</dt><dd><code>{consignmentId}</code></dd></div>
        <div><dt>寄存钱包</dt><dd><code>{consignment.depositorWallet}</code></dd></div>
        <div><dt>藏品类别</dt><dd>{consignmentCategoryLabels[consignment.category]}</dd></div>
        <div><dt>申报品相</dt><dd>{consignment.submittedCondition}</dd></div>
        <div><dt>凭证类型</dt><dd>{consignmentIdentityKindLabels[consignment.identityKind]}</dd></div>
        <div><dt>签发机构或厂商</dt><dd>{consignment.identityIssuer}</dd></div>
        <div><dt>实物编号</dt><dd>{consignment.identityNumber}</dd></div>
        <div><dt>提交时间</dt><dd><time dateTime={consignment.createdAt}>{dateFormatter.format(new Date(consignment.createdAt))}</time></dd></div>
        {consignment.inboundCarrier && <div><dt>寄入承运商</dt><dd>{consignment.inboundCarrier}</dd></div>}
        {consignment.inboundTrackingNumber && <div><dt>寄入运单</dt><dd>{consignment.inboundTrackingNumber}</dd></div>}
        {consignment.receivedAt && <div><dt>收货时间</dt><dd><time dateTime={consignment.receivedAt}>{dateFormatter.format(new Date(consignment.receivedAt))}</time></dd></div>}
        {consignment.reviewedAt && <div><dt>验收时间</dt><dd><time dateTime={consignment.reviewedAt}>{dateFormatter.format(new Date(consignment.reviewedAt))}</time></dd></div>}
        {consignment.rejectionReason && <div><dt>驳回原因</dt><dd>{consignment.rejectionReason}</dd></div>}
        {consignment.returnedAt && <div><dt>退回时间</dt><dd><time dateTime={consignment.returnedAt}>{dateFormatter.format(new Date(consignment.returnedAt))}</time></dd></div>}
        {consignment.returnTrackingNumber && <div><dt>退件运单</dt><dd>{consignment.returnTrackingNumber}</dd></div>}
        {itemId && <div><dt>入库实物编号</dt><dd><code>{itemId}</code></dd></div>}
      </dl>

      {status === "PENDING_ARRIVAL" && (
        <button type="button" className="warehouse-button" disabled={busy}
          onClick={() => onAction((signal) => receiveConsignment(token, consignmentId, signal))}>
          登记收货
        </button>
      )}

      {status === "RECEIVED" && (
        <div className="warehouse-actions">
          <form onSubmit={submitApproval}>
            <fieldset className="warehouse-action-fields" disabled={busy}>
              <legend>核验信息</legend>
              <label>核验藏品名称<input name="displayTitle" defaultValue={consignment.submittedTitle} required maxLength={120} /></label>
              <label>核验品相<textarea name="verifiedCondition" defaultValue={consignment.submittedCondition} rows={2} required maxLength={120} /></label>
              <label>内部仓位<input name="warehouseLocation" required maxLength={100} /></label>
              <button type="submit" className="warehouse-button">验收通过</button>
            </fieldset>
          </form>
          <details className="warehouse-reject">
            <summary>验真驳回</summary>
            <form onSubmit={submitRejection}>
              <fieldset className="warehouse-action-fields" disabled={busy}>
                <label>驳回原因<textarea name="rejectionReason" rows={3} required maxLength={500} /></label>
                <button type="submit" className="warehouse-button warehouse-button--secondary">确认驳回</button>
              </fieldset>
            </form>
          </details>
        </div>
      )}

      {status === "REJECTED_PENDING_RETURN" && (
        <form onSubmit={submitReturn}>
          <fieldset className="warehouse-action-fields" disabled={busy}>
            <label>退件运单号（选填）<input name="returnTrackingNumber" maxLength={120} /></label>
            <button type="submit" className="warehouse-button">登记退回</button>
          </fieldset>
        </form>
      )}
    </div>
  );
}

import { useLayoutEffect, useRef, useState } from "react";
import type { FormEvent } from "react";
import { createConsignment } from "../../features/consignments/api/createConsignment";
import type { CreateConsignmentRequest } from "../../features/consignments/api/createConsignment";
import { consignmentIdentityKindLabels, consignmentStatusLabels } from "../../features/consignments/consignmentTypes";
import type { Consignment, ConsignmentCategory, ConsignmentIdentityKind } from "../../features/consignments/consignmentTypes";
import { useWalletAuth } from "../../features/wallet-auth/WalletAuthProvider";
import { ApiError } from "../../shared/http/apiFetch";

/** 网络失败后重试同一份资料时保留的请求信息。 / Request identity retained when retrying unchanged data after a failure. */
type PendingSubmission = { payload: string; requestId: string };

/**
 * 用当前会话提交卡牌或潮玩的实物资料。 / Submit card or toy information for the current session.
 * @param token 保存在内存中的短期会话令牌。 / Short-lived session token held in memory.
 * @param onCreated 收到成功响应后更新列表与详情。 / Refresh list and details after the operation succeeds.
 */
export function ConsignForm({ token, onCreated }: { token: string; onCreated(record: Consignment): void }) {
  const { expireSession } = useWalletAuth();
  const [category, setCategory] = useState<ConsignmentCategory>("CARD");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [created, setCreated] = useState<Consignment | null>(null);
  const pending = useRef<PendingSubmission | null>(null);
  const activeRequest = useRef<AbortController | null>(null);

  // 退出或切换钱包后，旧响应不能修改新会话的表单与列表。
  // Stop responses from the previous session from updating the form or list after a wallet switch.
  useLayoutEffect(() => () => {
    activeRequest.current?.abort();
    activeRequest.current = null;
  }, []);

  /**
   * 提交表单，在确认成功前允许用同一个 UUID 重试。 / Submit the form and reuse its request UUID until success is confirmed.
   * @param event 浏览器的表单提交事件。 / Browser form submission event.
   */
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (activeRequest.current) return;
    const form = event.currentTarget;
    const data = new FormData(form);
    const values = readConsignmentForm(data, category);
    const payload = JSON.stringify(values);
    // 同步记录请求编号与控制器，拦截按钮更新前的快速重复点击。
    // Record the identifier and controller synchronously to guard against rapid duplicate clicks.
    if (pending.current?.payload !== payload) {
      pending.current = { payload, requestId: crypto.randomUUID() };
    }
    const controller = new AbortController();
    activeRequest.current = controller;
    setIsSubmitting(true);
    setError(null);
    setCreated(null);

    try {
      const result = await createConsignment(token, { ...values, clientRequestId: pending.current.requestId }, controller.signal);
      if (controller.signal.aborted) return;
      pending.current = null;
      form.reset();
      setCategory("CARD");
      setCreated(result);
      onCreated(result);
    } catch (failure) {
      if (controller.signal.aborted) return;
      if (failure instanceof ApiError && failure.status === 401) expireSession(token);
      setError(failure instanceof ApiError ? failure.message : "暂时无法提交寄存申请，请重试。");
    } finally {
      if (activeRequest.current === controller) {
        activeRequest.current = null;
        setIsSubmitting(false);
      }
    }
  }

  return (
    <section className="consign-panel" aria-labelledby="consign-form-title">
      <h2 id="consign-form-title">提交寄存申请</h2>
      <form className="consign-form" onSubmit={(event) => void submit(event)}>
        <fieldset disabled={isSubmitting}>
          <div className="consign-field-grid">
            <label className="consign-field">
              <span>藏品类别</span>
              <select name="category" value={category}
                onChange={(event) => setCategory(event.target.value as ConsignmentCategory)}>
                <option value="CARD">卡牌</option>
                <option value="TOY">潮玩</option>
              </select>
            </label>
            <label className="consign-field">
              <span>藏品名称</span>
              <input name="submittedTitle" maxLength={120} required />
            </label>
          </div>
          <label className="consign-field">
            <span>品相描述</span>
            <textarea name="submittedCondition" rows={3} maxLength={120} required />
          </label>
          <div className="consign-field-grid">
            <label className="consign-field">
              <span>凭据类型</span>
              <select name="identityKind" key={category}
                defaultValue={category === "CARD" ? "GRADING_CERTIFICATE" : "ITEM_SERIAL"}>
                {Object.entries(consignmentIdentityKindLabels).map(([value, label]) =>
                  <option key={value} value={value}>{label}</option>)}
              </select>
            </label>
            <label className="consign-field">
              <span>鉴定机构或厂商</span>
              <input name="identityIssuer" maxLength={100} required />
            </label>
          </div>
          <label className="consign-field">
            <span>证书编号或实物序列号</span>
            <input name="identityNumber" maxLength={120} required />
          </label>
          <div className="consign-field-grid">
            <label className="consign-field">
              <span>寄入承运商（选填）</span>
              <input name="inboundCarrier" maxLength={80} />
            </label>
            <label className="consign-field">
              <span>寄入运单号（选填）</span>
              <input name="inboundTrackingNumber" maxLength={120} />
            </label>
          </div>
        </fieldset>
        <button className="site-wallet__button" type="submit" disabled={isSubmitting}>
          {isSubmitting ? "提交中…" : "提交寄存申请"}
        </button>
        {error ? <p role="alert" className="consign-error">{error}</p> : null}
        {created ? (
          <div className="consign-notice" role="status">
            <strong>申请已受理</strong>
            <p>{created.submittedTitle} · {consignmentStatusLabels[created.status]}</p>
          </div>
        ) : null}
      </form>
    </section>
  );
}

/**
 * 从表单读取与后端匹配的业务字段，清理输入首尾空白。 / Collect backend request fields and trim surrounding whitespace.
 * @param data 当前表单的字段。 / Current form fields.
 * @param category 用户选择的藏品类别。 / Collectible category selected by the user.
 * @returns 用于请求比较与提交的资料。 / Payload used to compare retries and submit consignments.
 */
function readConsignmentForm(data: FormData, category: ConsignmentCategory): Omit<CreateConsignmentRequest, "clientRequestId"> {
  /** 读取一个纯文字字段。 / Read one plain text field.
   * @param name 表单字段名。 / Form field name.
   */
  const value = (name: string) => String(data.get(name) ?? "").trim();
  return {
    category,
    submittedTitle: value("submittedTitle"),
    submittedCondition: value("submittedCondition"),
    identityKind: value("identityKind") as ConsignmentIdentityKind,
    identityIssuer: value("identityIssuer"),
    identityNumber: value("identityNumber"),
    inboundCarrier: value("inboundCarrier") || undefined,
    inboundTrackingNumber: value("inboundTrackingNumber") || undefined,
  };
}

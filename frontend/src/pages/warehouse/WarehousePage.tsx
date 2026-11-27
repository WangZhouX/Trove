import { useQuery } from "@tanstack/react-query";
import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router";
import { useWalletAuth, warehouseOperatorRole } from "../../features/wallet-auth/WalletAuthProvider";
import { checkWarehouseAccess } from "../../features/warehouse/api/checkWarehouseAccess";
import { ApiError } from "../../shared/http/apiFetch";
import { WarehouseWorkbench } from "./WarehouseWorkbench";
import "./styles/warehouse.css";

/** 验证工作台时使用的已登录身份。 / Signed-in identity used to verify warehouse access. */
type ActiveWalletSession = NonNullable<ReturnType<typeof useWalletAuth>["session"]>;

/**
 * 使用后端的仓库授权结果控制工作台内容。 / Gate the warehouse workspace on the backend's authorization result.
 * @param props.session 当前已签名的钱包会话。 / Active signed-in wallet session.
 * @returns 权限验证结果及可用的工作台。 / Access feedback or the authorized workspace.
 */
function VerifiedWarehouseWorkspace({ session }: { session: ActiveWalletSession }) {
  const { expireSession } = useWalletAuth();
  const [denied, setDenied] = useState(false);
  const access = useQuery({
    // 令牌只随本次请求发送，查询缓存按身份和角色划分。 / Send the token only with the request and scope the query to identity and role.
    queryKey: ["warehouse-access", session.walletAddress, session.network, session.expiresAt, session.roles],
    queryFn: ({ signal }) => checkWarehouseAccess(session.token, signal),
    retry: false,
    gcTime: 0,
  });

  /**
   * 会话失效时退出，仓库权限撤销时卸载业务内容。 / Sign out invalid sessions and remove operational content when warehouse access is revoked.
   * @param error 当前账户的接口错误。 / Request error for the current account.
   * @returns 是否已作为认证或授权错误处理。 / Whether authentication or authorization handled the error.
   */
  const handleAuthError = useCallback((error: unknown): boolean => {
    if (!(error instanceof ApiError)) return false;
    if (error.status === 401) {
      expireSession(session.token);
      return true;
    }
    if (error.status === 403) {
      setDenied(true);
      return true;
    }
    return false;
  }, [expireSession, session.token]);

  useEffect(() => {
    if (access.error) handleAuthError(access.error);
  }, [access.error, handleAuthError]);

  if (denied || (access.error instanceof ApiError && access.error.status === 403)) {
    return (
      <div className="warehouse-status warehouse-status--denied">
        <p role="alert">当前钱包没有仓库权限。请在 Phantom 中切换到库管员账户并重新签名登录。</p>
        <Link to="/market">返回市场</Link>
      </div>
    );
  }

  if (access.isPending) return <p role="status">正在核验仓库权限…</p>;

  if (access.isError) {
    return (
      <div className="warehouse-status">
        <p role="alert">暂时无法核验仓库权限，请检查后端连接后重试。</p>
        <button type="button" className="site-wallet__button site-wallet__button--secondary" onClick={() => void access.refetch()}>
          重试验证
        </button>
      </div>
    );
  }

  const identity = access.data;
  if (identity.walletAddress !== session.walletAddress || identity.network !== session.network
      || !identity.roles.includes(warehouseOperatorRole)) {
    return <p role="alert">认证结果与当前钱包不一致，请退出后重新登录。</p>;
  }

  return (
    <>
      <p className="warehouse-verified">已通过库管员权限验证</p>
      <WarehouseWorkbench
        token={session.token}
        scope={[session.walletAddress, session.network, session.expiresAt]}
        onAuthError={handleAuthError}
      />
    </>
  );
}

/**
 * 直达仓库路由时提供签名入口，登录成功后向服务端核验权限。
 * Offer sign-in on direct warehouse navigation, then verify access with the server.
 * @returns 权限保护下的工作台页面。 / Warehouse page protected by authentication and authorization.
 */
export function WarehousePage() {
  const { session } = useWalletAuth();
  return (
    <section className="page warehouse-page">
      <h1>仓库工作台</h1>
      {session ? (
        <VerifiedWarehouseWorkspace key={`${session.walletAddress}:${session.network}:${session.expiresAt}`} session={session} />
      ) : (
        <div className="warehouse-status">
          <p>请使用已获授权的 Phantom 钱包签名登录，从顶部钱包按钮开始。</p>
        </div>
      )}
    </section>
  );
}

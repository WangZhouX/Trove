import type { WalletSessionInfo } from "../../wallet-auth/api/getWalletSession";
import { apiFetch } from "../../../shared/http/apiFetch";

/** 仓库工作台的后端授权结果。 / Backend authorization response for the warehouse workspace. */
export interface WarehouseAccessResponse {
  /** 授权通过的 Solana Base58 钱包地址。 / Solana Base58 wallet address that passed authorization. */
  walletAddress: string;
  /** 当前授权会话绑定的 Solana 网络。 / Solana network bound to the authorized session. */
  network: WalletSessionInfo["network"];
  /** 后端在本次请求校验的角色代码。 / Role codes checked by the backend for this request. */
  roles: string[];
}

/**
 * 进入仓库工作台时请求服务端校验当前会话权限。 / Asks the backend to check the current session on entry to the warehouse workspace.
 * @param token 当前钱包的会话令牌。 / Current wallet's session token.
 * @param signal 切换钱包或离开工作台时用于取消请求的信号。 / Signal to cancel the request on wallet switch or workspace exit.
 * @returns 通过校验的钱包身份与角色。 / Wallet identity and roles after successful authorization.
 * @throws 匿名或过期会话返回 401，普通用户返回 403。 / Anonymous or expired sessions get 401 and ordinary users get 403.
 */
export function checkWarehouseAccess(token: string, signal?: AbortSignal): Promise<WarehouseAccessResponse> {
  return apiFetch("/api/v1/warehouse/access", { token, signal });
}

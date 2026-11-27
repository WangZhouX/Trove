import { apiFetch } from "../../../shared/http/apiFetch";

/** 后端验证的钱包会话信息。 / Wallet session details verified by the backend. */
export interface WalletSessionInfo {
  /** 会话绑定的 Solana Base58 钱包地址。 / Solana Base58 wallet address bound to the session. */
  walletAddress: string;
  /** 会话绑定的 Solana 网络。 / Solana network bound to the session. */
  network: "devnet" | "localnet";
  /** 当前钱包由后端授予的角色代码。 / Roles granted to this wallet by the backend. */
  roles: string[];
  /** 会话过期时间，格式为 ISO 8601。 / Session expiration timestamp in ISO 8601. */
  expiresAt: string;
}

/**
 * 查询当前会话及最新角色，用于需要重新校验登录状态的页面。 / Reads the current session and roles when a page needs to recheck the sign-in state.
 * @param token 当前会话令牌。 / Current session token.
 * @param signal 离开页面时用于取消请求的信号。 / Signal to cancel the request on page exit.
 * @returns 后端验证的钱包、网络、角色和到期时间。 / Backend-verified wallet, network, roles, and expiration.
 */
export function getWalletSession(token: string, signal?: AbortSignal): Promise<WalletSessionInfo> {
  return apiFetch("/api/v1/auth/wallet/sessions/current", { token, signal });
}

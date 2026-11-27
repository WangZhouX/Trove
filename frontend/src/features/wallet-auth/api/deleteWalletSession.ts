import { apiFetch } from "../../../shared/http/apiFetch";

/**
 * 退出登录或切换钱包时撤销旧会话。 / Revokes the old session on sign-out or wallet switch.
 * @param token 待撤销的旧会话令牌。 / Old session token to revoke.
 * @returns 后端已撤销会话时完成。 / Resolves once the backend has revoked the session.
 */
export function deleteWalletSession(token: string): Promise<void> {
  return apiFetch("/api/v1/auth/wallet/sessions/current", { method: "DELETE", token });
}

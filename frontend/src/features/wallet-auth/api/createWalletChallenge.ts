import { apiFetch } from "../../../shared/http/apiFetch";
import type { WalletSessionInfo } from "./getWalletSession";

/** Phantom 登录挑战的请求参数。 / Request for a Phantom sign-in challenge. */
export interface CreateWalletChallengeRequest {
  /** 当前 Phantom 账户的 Solana Base58 公钥。 / Solana Base58 public key of the current Phantom account. */
  walletAddress: string;
  /** 登录消息绑定的 Solana 网络。 / Solana network bound to the sign-in message. */
  network: WalletSessionInfo["network"];
}

/** 后端签发的一次性签名挑战。 / One-time signature challenge issued by the backend. */
export interface CreateWalletChallengeResponse {
  /** 后端创建的挑战 UUID。 / Challenge UUID assigned by the backend. */
  challengeId: string;
  /** 签名验证时原样回传的一次性随机值。 / One-time random value to return verbatim at verification. */
  nonce: string;
  /** Phantom 应当按 UTF-8 原样签名的消息。 / Exact message for Phantom to sign as UTF-8. */
  message: string;
  /** 挑战过期时间，格式为 ISO 8601。 / Challenge expiration timestamp in ISO 8601. */
  expiresAt: string;
}

/**
 * 用户连接钱包并主动登录时申请签名挑战。 / Requests a signature challenge when the user explicitly signs in with a connected wallet.
 * @param request 当前钱包地址及 Solana 网络。 / Current wallet address and Solana network.
 * @param signal 钱包切换时用于取消请求的信号。 / Signal to cancel the request on wallet switch.
 * @returns 要签名的消息、挑战标识、随机值和过期时间。 / Message to sign, challenge identifier, nonce, and expiration time.
 */
export function createWalletChallenge(
  request: CreateWalletChallengeRequest,
  signal?: AbortSignal,
): Promise<CreateWalletChallengeResponse> {
  return apiFetch("/api/v1/auth/wallet/challenges", { method: "POST", body: request, signal });
}

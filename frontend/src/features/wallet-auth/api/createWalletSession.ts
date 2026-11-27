import { apiFetch } from "../../../shared/http/apiFetch";
import type { WalletSessionInfo } from "./getWalletSession";

/** 钱包签名验证和会话创建请求。 / Request to verify a wallet signature and create a session. */
export interface CreateWalletSessionRequest {
  /** 当前请求对应的服务端挑战 UUID。 / Backend challenge UUID for this request. */
  challengeId: string;
  /** 签名钱包的 Solana Base58 公钥。 / Solana Base58 public key of the signing wallet. */
  walletAddress: string;
  /** 与签名挑战一致的 Solana 网络。 / Solana network matching the challenge. */
  network: WalletSessionInfo["network"];
  /** 签发挑战时返回的一次性随机值。 / One-time random value returned with the challenge. */
  nonce: string;
  /** UTF-8 消息的 Ed25519 签名，采用标准 Base64 编码。 / Ed25519 signature over the UTF-8 message, encoded as standard Base64. */
  signature: string;
}

/** 成功验证钱包后返回的会话与令牌。 / Session and token returned after successful wallet verification. */
export interface CreateWalletSessionResponse extends WalletSessionInfo {
  /** 页面内存中的 Bearer 令牌，用于后续请求。 / In-memory Bearer token used for subsequent requests. */
  token: string;
}

/**
 * 用户在 Phantom 确认签名后用挑战和签名换取会话。 / Exchanges the challenge and signature for a session after Phantom approval.
 * @param request 挑战标识、签名者、网络、随机值及签名。 / Challenge identifier, signer, network, nonce, and signature.
 * @param signal 用于取消请求的信号。 / Signal that can cancel the request.
 * @returns 已验证的钱包会话及后端签发的令牌。 / Verified wallet session and backend-issued token.
 */
export function createWalletSession(
  request: CreateWalletSessionRequest,
  signal?: AbortSignal,
): Promise<CreateWalletSessionResponse> {
  return apiFetch("/api/v1/auth/wallet/sessions", { method: "POST", body: request, signal });
}

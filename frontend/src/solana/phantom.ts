/** Phantom 暴露的 Solana 公钥，转换为 Base58。 / Phantom Solana key with Base58 string conversion. */
type PhantomPublicKey = { toString(): string };

/** Phantom 钱包连接、认证签名和账户事件。 / Phantom connection, authentication signatures, and account events. */
export interface PhantomProvider {
  /** 注入对象的钱包标记。 / Wallet identifier on the injected object. */
  readonly isPhantom: boolean;
  /** 当前向站点公开的公钥，断开时为空。 / Current key exposed to the site, null when disconnected. */
  readonly publicKey: PhantomPublicKey | null;

  /** 请求连接当前账户。 / Request connection to the selected account.
   * @returns 用户授权的公钥。 / The user-authorized public key.
   */
  connect(): Promise<{ publicKey: PhantomPublicKey }>;
  /** 关闭当前站点的连接。 / Disconnect the current site. */
  disconnect(): Promise<void>;
  /** 请求用户对登录消息签名。 / Request the user's signature on a login message.
   * @param message 后端挑战消息的 UTF-8 字节。 / UTF-8 bytes of the backend challenge message.
   * @param encoding 用来显示消息的编码。 / Encoding for displaying the message.
   * @returns Ed25519 原始签名字节，以及钱包提供时的签名公钥。 / Raw Ed25519 signature and, when provided by the wallet, the signer key.
   */
  signMessage(message: Uint8Array, encoding: "utf8"): Promise<{
    publicKey?: PhantomPublicKey;
    signature: Uint8Array;
  }>;

  /** 监听钱包事件。 / Subscribe to a wallet event.
   * @param event 账户变更或连接关闭。 / Account change or disconnection.
   * @param handler 监听函数，公钥为空时新账户尚未连接。 / Event listener. A null key means the new account is not connected.
   */
  on(event: "accountChanged" | "disconnect", handler: (key?: PhantomPublicKey | null) => void): void;
  /** 移除已注册的监听函数。 / Remove a registered listener.
   * @param event 监听函数所对应的事件。 / Event associated with the listener.
   * @param handler 之前注册的同一个监听函数。 / The same listener previously registered.
   */
  off(event: "accountChanged" | "disconnect", handler: (key?: PhantomPublicKey | null) => void): void;
}

declare global {
  interface Window {
    /** 浏览器扩展注入的 Phantom 命名空间。 / Phantom namespace injected by the browser extension. */
    phantom?: { solana?: PhantomProvider };
  }
}

/**
 * 静默读取 Phantom 的 Solana provider。 / Read Phantom's Solana provider silently.
 * @returns Phantom 未注入时为空。 / Null when Phantom is unavailable.
 */
export function getPhantomProvider(): PhantomProvider | null {
  const provider = window.phantom?.solana;
  return provider?.isPhantom ? provider : null;
}

/**
 * 对后端返回的消息原文签名，并检查签名公钥和预期公钥相同。
 * Sign the exact backend message and check that the signing key matches the expected key.
 * @param provider 当前连接的 Phantom。 / The connected Phantom provider.
 * @param message 未修改的挑战文本。 / The unchanged challenge text.
 * @param walletAddress 发起挑战的公钥，Base58 格式。 / Base58 public key used to start the challenge.
 * @returns 标准 Base64 编码的签名。 / Standard Base64-encoded signature.
 * @throws 钱包拒签、签名格式错误或签名期间切换账户。 / On refusal, a malformed signature, or an account switch during signing.
 */
export async function signPhantomChallenge(
  provider: PhantomProvider,
  message: string,
  walletAddress: string,
): Promise<string> {
  const signed = await provider.signMessage(new TextEncoder().encode(message), "utf8");
  const signerAddress = signed.publicKey?.toString() ?? walletAddress;
  if (signerAddress !== walletAddress || provider.publicKey?.toString() !== walletAddress) {
    throw new Error("签名期间切换了钱包，请重新登录。");
  }
  if (signed.signature.length !== 64) {
    throw new Error("Phantom 返回了无效的 Ed25519 签名。");
  }
  return btoa(String.fromCharCode(...signed.signature));
}

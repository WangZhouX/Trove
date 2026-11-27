import { useQueryClient } from "@tanstack/react-query";
import { createContext, useCallback, useContext, useEffect, useRef, useState } from "react";
import type { ReactNode } from "react";
import { appConfig } from "../../shared/config/appConfig";
import { ApiError } from "../../shared/http/apiFetch";
import { getPhantomProvider, signPhantomChallenge } from "../../solana/phantom";
import { createWalletChallenge } from "./api/createWalletChallenge";
import { createWalletSession } from "./api/createWalletSession";
import { deleteWalletSession } from "./api/deleteWalletSession";
import { getWalletSession } from "./api/getWalletSession";

/** 后端验证通过的短期会话，只驻留内存。 / Backend-verified short-lived session, kept only in memory. */
type WalletSession = Awaited<ReturnType<typeof createWalletSession>>;

/** 登录阶段，用于反馈当前需要处理的步骤。 / Login stages for showing the current action. */
type LoginStage = "connecting" | "challenging" | "signing" | "verifying";

/** 一次登录尝试。账户或连接变化时使旧尝试失效。 / One login attempt, invalidated when the account or connection changes. */
interface LoginAttempt {
  /** 登录挑战请求的取消控制器。 / Cancellation for the challenge request. */
  abortController: AbortController;
  /** 已选定用于签名的公钥。连接完成前为空。 / Selected signing key, null until connected. */
  walletAddress: string | null;
}

interface WalletAuthState {
  /** Phantom 向站点公开的公钥。 / Phantom public key exposed to the site. */
  walletAddress: string | null;
  /** 当前登录会话。 / Active login session. */
  session: WalletSession | null;
  /** 进行中的登录步骤。 / Current login stage. */
  stage: LoginStage | null;
  /** 取消、切换或请求失败后的说明。 / Feedback on cancellation, switching, or request failure. */
  notice: string | null;
  /** 用户主动连接并签名登录。 / Connect and sign in after a user gesture. */
  signIn(): Promise<void>;
  /** 结束会话并断开当前站点连接。 / End the session and disconnect the current site. */
  signOut(): void;
  /** 认证失败时清除仍在使用的匹配会话。 / Clear the matching active session after failed authentication.
   * @param token 后端拒绝的会话令牌。 / Session token rejected by the backend.
   */
  expireSession(token: string): void;
}

/** 应用内页面共享的钱包身份上下文。 / Wallet identity context shared by application pages. */
const WalletAuthContext = createContext<WalletAuthState | null>(null);

/** 后端授予的仓库操作角色代码。 / Backend role granting warehouse operations. */
export const warehouseOperatorRole = "WAREHOUSE_OPERATOR";

/**
 * 取消已离开的会话。在网络中断时等待后端 TTL 回收。
 * Revoke a retired session. Backend TTL reclaims it if the network is unavailable.
 * @param token 待撤销的会话令牌，仅发给后端。 / Session token sent only to the backend for revocation.
 */
function revokeSession(token: string): void {
  void deleteWalletSession(token).catch(() => undefined);
}

/**
 * 将钱包拒绝或常见连接错误变成可操作的说明。 / Turn common wallet errors into actionable feedback.
 * @param error 捕获的登录异常。 / Caught login error.
 * @returns 可以直接在界面提示的消息。 / Message suitable for display.
 */
function getLoginError(error: unknown): string {
  if (typeof error === "object" && error !== null && "code" in error) {
    if (error.code === 4001) return "你已取消钱包请求，可以重试。";
    if (error.code === -32002) return "请先处理 Phantom 中已有的请求，然后重试。";
  }
  return error instanceof Error ? error.message : "钱包登录失败，请重试。";
}

/**
 * 管理钱包选择、登录和旧账号的会话清理。 / Manage wallet selection, sign-in, and prior-session cleanup.
 * @param children 使用当前钱包身份的站点页面。 / Site pages using the current wallet identity.
 * @returns 带钱包身份上下文的应用。 / Application with wallet identity context.
 */
export function WalletAuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient();
  const [provider, setProvider] = useState(getPhantomProvider);
  const [walletAddress, setWalletAddress] = useState<string | null>(null);
  const [session, setSession] = useState<WalletSession | null>(null);
  const [stage, setStage] = useState<LoginStage | null>(null);
  const [notice, setNotice] = useState<string | null>(null);

  // 事件和异步结果使用同步引用判断是否仍属于当前账户。 / Events and async results use refs to check the current account synchronously.
  const addressRef = useRef<string | null>(null);
  const sessionRef = useRef<WalletSession | null>(null);
  const attemptRef = useRef<LoginAttempt | null>(null);

  /** 清理旧身份的请求、会话及缓存。 / Clear requests, session, and cache for the prior identity. */
  const clearAuthentication = useCallback(() => {
    attemptRef.current?.abortController.abort();
    attemptRef.current = null;
    const previousSession = sessionRef.current;
    sessionRef.current = null;
    setSession(null);
    setStage(null);
    queryClient.clear();
    if (previousSession) revokeSession(previousSession.token);
  }, [queryClient]);

  /**
   * 根据事件公开的新公钥清理旧身份。 / Clear the old identity when Phantom publishes an account change.
   * @param nextAddress 新账户公钥，断开或尚未授权时为空。 / New public key, null when disconnected or not authorized.
   */
  const observeWallet = useCallback((nextAddress: string | null) => {
    if (nextAddress === addressRef.current) return;
    const previousAddress = addressRef.current;
    addressRef.current = nextAddress;
    setWalletAddress(nextAddress);

    // 首次连接可以在请求完成前公开公钥。随后仍核对 connect() 返回的签名账户。
    // First connection can publish its key before resolving. The connect result is still checked against the selected key.
    if (!previousAddress && nextAddress && attemptRef.current?.walletAddress === null) return;

    clearAuthentication();
    setNotice(nextAddress ? "钱包已切换，请用当前账户签名登录。" : "Phantom 已断开，请重新连接。");
  }, [clearAuthentication]);

  useEffect(() => {
    if (!provider) return;

    /**
     * 接收当前站点可用的账户公钥。 / Read the public key available to this site.
     * @param key Phantom 事件中的公钥。 / The key reported by Phantom.
     */
    const onAccountChanged: Parameters<typeof provider.on>[1] = (key) => {
      observeWallet(key?.toString() ?? null);
    };
    /** 钱包主动断开时移除旧权限，包括仍在签名的尝试。 / Drop prior access and pending sign-in when Phantom disconnects. */
    const onDisconnect = () => {
      if (addressRef.current) {
        observeWallet(null);
      } else if (attemptRef.current) {
        clearAuthentication();
        setNotice("Phantom 已断开，请重新连接。");
      }
    };
    provider.on("accountChanged", onAccountChanged);
    provider.on("disconnect", onDisconnect);
    return () => {
      provider.off("accountChanged", onAccountChanged);
      provider.off("disconnect", onDisconnect);
    };
  }, [provider, clearAuthentication, observeWallet]);

  useEffect(() => () => {
    attemptRef.current?.abortController.abort();
    attemptRef.current = null;
    const previousSession = sessionRef.current;
    sessionRef.current = null;
    if (previousSession) revokeSession(previousSession.token);
  }, []);

  /**
   * 失效当前会话，忽略旧请求的 401。 / Expire the current session and ignore a 401 from an old request.
   * @param token 服务端拒绝或已过期的令牌。 / Token rejected by the server or expired locally.
   */
  const expireSession = useCallback((token: string) => {
    if (sessionRef.current?.token !== token) return;
    clearAuthentication();
    setNotice("登录会话已失效，请重新签名登录。");
  }, [clearAuthentication]);

  const sessionToken = session?.token;
  const sessionExpiresAt = session?.expiresAt;
  useEffect(() => {
    if (!sessionToken || !sessionExpiresAt) return;
    const timeout = window.setTimeout(
      () => expireSession(sessionToken),
      Math.max(0, Date.parse(sessionExpiresAt) - Date.now()),
    );

    // 回到站点时更新服务端角色，停用账户的结果不会保留在导航中。
    // Refresh roles on return to the site so revoked access does not linger in the navigation.
    let refresh: AbortController | null = null;
    /** 重新读取仍属于当前钱包的会话。 / Re-read the session while it still belongs to the current wallet. */
    const refreshSession = () => {
      const selectedAddress = provider?.publicKey?.toString() ?? null;
      if (selectedAddress !== addressRef.current) {
        observeWallet(selectedAddress);
        return;
      }
      refresh?.abort();
      const controller = new AbortController();
      refresh = controller;
      void getWalletSession(sessionToken, controller.signal).then((info) => {
        const current = sessionRef.current;
        if (controller.signal.aborted || current?.token !== sessionToken) return;
        if (current.walletAddress !== info.walletAddress || current.network !== info.network) {
          expireSession(sessionToken);
          return;
        }
        const updatedSession = { ...info, token: sessionToken };
        sessionRef.current = updatedSession;
        setSession(updatedSession);
      }).catch((error: unknown) => {
        if (!controller.signal.aborted && error instanceof ApiError && error.status === 401) {
          expireSession(sessionToken);
        }
      });
    };

    window.addEventListener("focus", refreshSession);
    return () => {
      refresh?.abort();
      window.removeEventListener("focus", refreshSession);
      window.clearTimeout(timeout);
    };
  }, [sessionToken, sessionExpiresAt, expireSession, observeWallet, provider]);

  /** 用户点击时连接 Phantom 并提交登录签名。 / Connect Phantom and submit a login signature after a user click. */
  async function signIn(): Promise<void> {
    if (attemptRef.current || sessionRef.current) return;
    const wallet = getPhantomProvider();
    if (!wallet) {
      setNotice("未检测到 Phantom。请在已安装 Phantom 的浏览器中打开此页面。");
      return;
    }

    setProvider(wallet);
    setStage("connecting");
    setNotice(null);
    const attempt: LoginAttempt = { abortController: new AbortController(), walletAddress: null };
    attemptRef.current = attempt;

    try {
      const connection = await wallet.connect();
      if (attemptRef.current !== attempt) return;
      const selectedAddress = connection.publicKey.toString();
      if (wallet.publicKey?.toString() !== selectedAddress) {
        throw new Error("连接期间切换了钱包，请重新登录。");
      }
      attempt.walletAddress = selectedAddress;
      addressRef.current = selectedAddress;
      setWalletAddress(selectedAddress);
      const identity = { walletAddress: selectedAddress, network: appConfig.network };

      setStage("challenging");
      const challenge = await createWalletChallenge(identity, attempt.abortController.signal);
      if (attemptRef.current !== attempt) return;
      setStage("signing");
      const signature = await signPhantomChallenge(wallet, challenge.message, selectedAddress);
      if (attemptRef.current !== attempt) return;

      setStage("verifying");
      // 收到旧请求新建的令牌后立即撤销，避免切换时遗留会话。
      // Revoke tokens returned to stale login requests so account switches leave no active prior session behind.
      const result = await createWalletSession({
        ...identity,
        challengeId: challenge.challengeId,
        nonce: challenge.nonce,
        signature,
      });
      if (attemptRef.current !== attempt) {
        revokeSession(result.token);
        return;
      }
      const currentAddress = wallet.publicKey?.toString() ?? null;
      if (currentAddress !== selectedAddress) {
        revokeSession(result.token);
        observeWallet(currentAddress);
        return;
      }
      if (result.walletAddress !== selectedAddress || result.network !== appConfig.network) {
        revokeSession(result.token);
        throw new Error("服务器返回的会话与当前钱包不一致，请重新登录。");
      }
      sessionRef.current = result;
      setSession(result);
    } catch (error) {
      if (attemptRef.current === attempt) setNotice(getLoginError(error));
    } finally {
      if (attemptRef.current === attempt) {
        attemptRef.current = null;
        setStage(null);
      }
    }
  }

  /** 立即退出服务端身份，并尝试断开钱包。 / Retire the server identity immediately and attempt to disconnect the wallet. */
  function signOut(): void {
    clearAuthentication();
    addressRef.current = null;
    setWalletAddress(null);
    setNotice(null);
    void provider?.disconnect().catch(() => undefined);
  }

  return (
    <WalletAuthContext.Provider value={{ walletAddress, session, stage, notice, signIn, signOut, expireSession }}>
      {children}
    </WalletAuthContext.Provider>
  );
}

/**
 * 在应用内读取当前钱包状态和登录方法。 / Read the active wallet state and auth actions within the app.
 * @returns 提供器发布的状态。 / Published auth provider state.
 * @throws 提供器不存在时抛出。 / When rendered outside the provider.
 */
export function useWalletAuth(): WalletAuthState {
  const walletAuth = useContext(WalletAuthContext);
  if (!walletAuth) throw new Error("WalletAuthProvider is required.");
  return walletAuth;
}

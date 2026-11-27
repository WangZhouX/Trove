import { useWalletAuth, warehouseOperatorRole } from "../../features/wallet-auth/WalletAuthProvider";

/** 登录进行中要显示的操作进度。 / Visible progress labels while signing in. */
const stageLabels = {
  connecting: "正在连接 Phantom…",
  challenging: "正在获取登录挑战…",
  signing: "请在 Phantom 中签名…",
  verifying: "正在验证签名…",
};

/**
 * 在导航栏显示钱包身份、登录进度和退出入口。 / Show wallet identity, login progress, and logout in the header.
 * @returns 当前 Phantom 账户对应的控件。 / Controls for the active Phantom account.
 */
export function WalletControl() {
  const { walletAddress, session, stage, signIn, signOut } = useWalletAuth();
  const roleLabel = session?.roles.includes(warehouseOperatorRole) ? "库管员" : "普通账户";
  const loginLabel = walletAddress ? "签名登录" : "连接 Phantom";

  return (
    <div className="site-wallet">
      {walletAddress && (
        <span className="site-wallet__identity" title={walletAddress}>
          {session && `${roleLabel} `}
          <code>{walletAddress.slice(0, 4)}…{walletAddress.slice(-4)}</code>
        </span>
      )}
      {session ? (
        <button type="button" className="site-wallet__button site-wallet__button--secondary" onClick={signOut}>
          退出登录
        </button>
      ) : (
        <button type="button" className="site-wallet__button" disabled={stage !== null} onClick={signIn}>
          {stage ? stageLabels[stage] : loginLabel}
        </button>
      )}
    </div>
  );
}

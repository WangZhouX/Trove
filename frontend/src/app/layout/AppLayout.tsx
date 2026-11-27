import { NavLink, Outlet } from "react-router";
import { useWalletAuth, warehouseOperatorRole } from "../../features/wallet-auth/WalletAuthProvider";
import { appConfig } from "../../shared/config/appConfig";
import { WalletControl } from "./WalletControl";

/** 所有用户可以看到的站点导航。 / Site navigation visible to all users. */
const navigation = [
  { path: "/market", label: "市场" },
  { path: "/collection", label: "我的收藏" },
  { path: "/consign", label: "寄存" },
  { path: "/redemption", label: "兑换进度" },
];

/**
 * 展示全站导航和钱包登录控件。 / Show site navigation and wallet login controls.
 * @returns 含当前角色入口的布局。 / Layout with navigation for the current role.
 */
export function AppLayout() {
  const { session, notice } = useWalletAuth();
  return (
    <>
      <a className="skip-link" href="#main-content">跳到正文</a>
      <header className="site-header">
        <div className="site-header__inner">
          <NavLink className="site-brand" to="/market" end>Trove</NavLink>
          <nav aria-label="主导航">
            {navigation.map(({ path, label }) => (
              <NavLink key={path} to={path}>{label}</NavLink>
            ))}
            {session?.roles.includes(warehouseOperatorRole) && (
              <NavLink to="/warehouse">仓库工作台</NavLink>
            )}
          </nav>
          <div className="site-account">
            <span className="site-network">{appConfig.networkLabel}</span>
            <WalletControl />
          </div>
        </div>
        {notice && <p className="site-wallet-notice" role="status">{notice}</p>}
      </header>
      <main className="site-main" id="main-content" tabIndex={-1}>
        <Outlet />
      </main>
    </>
  );
}

import { NavLink, Outlet } from "react-router";
import { appConfig } from "../../shared/config/appConfig";

const navigation = [
  { path: "/market", label: "市场" },
  { path: "/collection", label: "我的收藏" },
  { path: "/consign", label: "寄存" },
  { path: "/redemption", label: "兑换进度" },
  { path: "/warehouse", label: "仓库工作台" },
];

export function AppLayout() {
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
          </nav>
          <span className="site-network">{appConfig.networkLabel}</span>
        </div>
      </header>
      <main className="site-main" id="main-content" tabIndex={-1}>
        <Outlet />
      </main>
    </>
  );
}

import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { BrowserRouter } from "react-router";
import { WalletAuthProvider } from "../features/wallet-auth/WalletAuthProvider";
import { AppRoutes } from "./AppRoutes";

// 单个查询客户端让页面导航共享缓存。 One client keeps the query cache across navigation.
const queryClient = new QueryClient();

/**
 * 为整个站点建立查询缓存和钱包身份。 / Provide query caching and wallet identity for the whole site.
 * @returns Trove 的路由与页面。 / Trove's routes and pages.
 */
export function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <WalletAuthProvider>
        <BrowserRouter>
          <AppRoutes />
        </BrowserRouter>
      </WalletAuthProvider>
    </QueryClientProvider>
  );
}

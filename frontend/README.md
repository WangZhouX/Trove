# Trove 前端

React、TypeScript 和 Vite 应用，提供页面路由、TanStack Query、Devnet 公开配置、Phantom 签名登录及库管员工作台入口。链上交易后续使用与链上程序匹配的 Anchor TypeScript 客户端，当前目标版本为 `@anchor-lang/core@1.2.2`，并按 Anchor 文档搭配 Web3.js v1。

## 本地运行

在本目录使用 WSL Linux Node.js 运行。当前 WSL 的 Node.js 已安装在 `~/.local/bin`。若终端找不到 `node` 或 `npm`，先执行 `export PATH="$HOME/.local/bin:$PATH"`。

```bash
npm ci
npm run dev
```

Vite 默认访问地址为 `http://localhost:5173`。执行 `npm run build` 会先检查 TypeScript 类型，再输出 `dist/`。

默认后端地址为 `http://127.0.0.1:8080`，Solana RPC 为 Devnet。若要覆盖默认值，将 `.env.example` 复制为 `.env.local` 后修改。`VITE_` 开头的值会进入浏览器构建产物。

## 钱包登录和仓库权限

1. 用安装了 Phantom 的 Chrome 打开 `http://localhost:5173`。在顶栏点击「连接 Phantom」并批准连接后，查看并签署后端发出的一次性登录消息。
2. 若后端为当前公钥授予 `WAREHOUSE_OPERATOR`，顶栏显示「仓库工作台」。进入时，页面向后端重新验证权限。普通账户可直接打开 `/warehouse` 查看权限不足说明。
3. 在 Phantom 中切换账户、主动断开或点击「退出登录」会清理会话和账号查询缓存。切换后按顶栏提示用当前账户重新签名。刷新页面后，用户重新发起登录。

会话令牌保存在页面内存，随后以 Bearer 令牌发送给 Trove 后端。Phantom 签名用于证明公钥所有权。角色由后端判断。Vite 页面地址变化时，需要同步后端的前端来源白名单。

## 目录

- `src/app/` 管理应用入口、路由、全局布局和样式。
- `src/pages/` 按市场、收藏、寄存、兑换和仓库分目录存放页面。
- `src/features/wallet-auth/` 管理登录与会话。仓库权限 API 位于 `src/features/warehouse/api/`。
- `src/solana/` 封装 Phantom provider 的连接、消息签名和账户事件。
- `src/shared/config/` 保存后端地址和 Solana RPC 等公开环境配置。

部署静态产物时，站点需要对应用路由回退到 `index.html`，以支持直接打开或刷新页面路径。

项目概况见 [Trove](../README.md)。

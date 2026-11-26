# Trove 前端

React、TypeScript 和 Vite 应用。当前脚手架提供页面路由、TanStack Query 缓存入口和 Devnet 的公开配置，钱包授权、接口请求和业务交易在对应功能中接入。接入时使用与链上程序匹配的 Anchor TypeScript 客户端，当前目标版本为 `@anchor-lang/core@1.2.2`，并按 Anchor 文档搭配 Web3.js v1。

## 本地运行

在本目录使用 WSL Linux Node.js 运行。当前 WSL 的 Node.js 已安装在 `~/.local/bin`。若终端找不到 `node` 或 `npm`，先执行 `export PATH="$HOME/.local/bin:$PATH"`。

```bash
npm ci
npm run dev
```

Vite 默认访问地址为 `http://localhost:5173`。执行 `npm run build` 会先检查 TypeScript 类型，再输出 `dist/`。

默认后端地址为 `http://127.0.0.1:8080`，Solana RPC 为 Devnet。若要覆盖默认值，将 `.env.example` 复制为 `.env.local` 后修改。`VITE_` 开头的值会进入浏览器构建产物。

## 目录

- `src/app/` 管理应用入口、路由、全局布局和样式。
- `src/pages/` 按市场、收藏、寄存、兑换和仓库分目录存放页面。
- `src/shared/config/` 保存后端地址和 Solana RPC 等公开环境配置。

部署静态产物时，站点需要对应用路由回退到 `index.html`，以支持直接打开或刷新页面路径。

项目概况见 [Trove](../README.md)。

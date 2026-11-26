// Vite 环境变量会进入浏览器产物，这里只读取公开地址。 Vite env values are public browser configuration.
export const appConfig = {
  apiBaseUrl: import.meta.env.VITE_API_BASE_URL?.trim() || "http://127.0.0.1:8080",
  solanaRpcUrl: import.meta.env.VITE_SOLANA_RPC_URL?.trim() || "https://api.devnet.solana.com",
  networkLabel: "Solana Devnet",
} as const;

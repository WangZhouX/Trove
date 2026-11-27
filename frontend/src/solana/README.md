# Solana 客户端

`phantom.ts` 定义浏览器注入的 Solana 钱包连接、签名和事件接口。认证流程由 `src/features/wallet-auth/` 管理。钱包签署后端挑战的 UTF-8 字节，签名按标准 Base64 发回服务端。

[Phantom 连接文档](https://docs.phantom.com/solana/establishing-a-connection) · [Phantom 消息签名](https://docs.phantom.com/solana/signing-a-message)

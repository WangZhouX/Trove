# 后端工具

`SolanaWalletUtil` 检查 Solana 地址格式，并用 JDK 的 Ed25519 实现验证钱包签名。登录服务负责挑战的规范消息、有效期与消费规则。运行 `./mvnw clean package` 可随应用编译此工具。

# Backend utilities

`SolanaWalletUtil` checks Solana address formatting and verifies wallet signatures through the JDK Ed25519 implementation. The sign-in service owns challenge message construction, expiry, and consumption. Run `./mvnw clean package` to compile the utility with the application.

验签会拒绝小阶公钥。相关坐标见 libsodium 的 Ed25519 参考实现。 Verification rejects small-order keys. The coordinates are documented in the libsodium Ed25519 reference implementation. [libsodium `ge25519_has_small_order`](https://github.com/jedisct1/libsodium/blob/1.0.20-RELEASE/src/libsodium/crypto_core/ed25519/ref10/ed25519_ref10.c#L998)

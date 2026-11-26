# Trove 链上程序

`programs/trove/src/lib.rs` 是 Anchor 程序入口，`instructions/` 存放指令及账户约束。当前的 `ping` 指令需要调用者签名，用来验证程序入口和 IDL。账户模型和错误定义随业务指令加入 `state/` 和 `errors.rs`。项目概况见 [Trove](../README.md)。

## 构建

在 WSL 的 `Trove/solana` 下运行以下命令。构建用的 Anchor CLI 为 1.2.2，Solana CLI 使用 Agave 4.1.2。Anchor 使用 Solana SBF 工具链生成链上 ELF，使用宿主工具链生成 IDL。

```bash
(
  umask 077
  anchor build
)
```

- `target/deploy/trove.so` 是 SBF 程序文件。
- `target/idl/trove.json` 是描述指令和账户的 Anchor IDL。
- `target/types/trove.ts` 是生成的 TypeScript 类型。
- `target/deploy/trove-keypair.json` 是本机程序密钥对，位于忽略目录。保管部署所使用的程序密钥对和升级钱包。

默认配置使用 Localnet。首次构建缺少程序密钥对时，Anchor 会创建本机密钥对，并提示它的公钥与源码里的公开地址不同。产物仍使用源码地址。构建不需要运行验证器，部署时才需要同步到实际使用的程序地址。

## 首次部署到 Devnet 时

1. 准备专用的 Devnet 部署钱包，并使用该钱包领取测试 SOL。
2. 使用 `solana-keygen pubkey target/deploy/trove-keypair.json` 核对程序密钥对的公钥。新检出目录可能生成新的本机密钥对，部署前使用 `anchor keys sync` 同步源码地址和 Localnet 配置，并将同一公钥写入 `Anchor.toml` 的 `[programs.devnet]`。
3. 重新运行 `anchor build`，核对生成的 IDL 地址。部署时用 `anchor program deploy --provider.cluster devnet --provider.wallet <钱包文件路径>` 显式指定网络和钱包。部署完成后将公共程序地址和对应 IDL 同步给前端和后端。

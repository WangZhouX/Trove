# Trove

Trove 是一个基于 Solana 的实体收藏品市场演示项目。卡牌和潮玩从仓库验收入库后对应一枚 NFT。用户可以用钱包购买、转售或兑换实物，仓库根据已确认的兑换凭证处理寄回。

当前提供三个可独立构建的模块，已接入 Phantom 签名登录和仓库工作台的角色校验。寄存、交易和兑换是后续业务实现范围。演示以 Solana Devnet、Phantom 和测试 SOL 为目标环境。

| 模块 | 技术栈 | 构建命令 |
| --- | --- | --- |
| [前端](frontend/README.md) | React、TypeScript、Vite、React Router、TanStack Query | `npm ci` 后执行 `npm run build` |
| [后端](backend/README.md) | Spring Boot、OpenJDK 25、PostgreSQL、MyBatis-Plus、Flyway | `./mvnw clean package` |
| [链上程序](solana/README.md) | Rust、Anchor、Solana | `anchor build` |

在 WSL 中进入对应模块目录运行构建命令。各模块的 README 说明环境变量、运行方式和构建产物。

[演示数据](demo-data/README.md) 提供卡牌、潮玩、盲盒奖池和模拟物流样例，用于手动演示和联调。

项目采用 [MIT 协议](LICENSE)。

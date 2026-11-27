# Trove 后端

Spring Boot 4.2、OpenJDK 25、PostgreSQL、MyBatis-Plus、Flyway、Spring Security 和 springdoc OpenAPI。后端采用单应用 Maven 构建，默认入口为 `io.trove.backend.TroveApplication`。

## 项目结构

后端以 `src/main/java/io/trove/backend/` 为基础包，使用 `Controller → Service → Mapper → PostgreSQL` 的依赖方向。

| 包 | 内容 |
| --- | --- |
| [controller](src/main/java/io/trove/backend/controller/README.md) | 钱包认证、仓库权限核验和其余业务入口。 |
| [service](src/main/java/io/trove/backend/service/README.md) 和 [service.impl](src/main/java/io/trove/backend/service/impl/README.md) | 钱包签名验证、会话、角色查询及表级服务。 |
| [mapper](src/main/java/io/trove/backend/mapper/README.md) | 15 个 MyBatis-Plus Mapper 接口，钱包身份、挑战、角色已接入。 |
| [entity](src/main/java/io/trove/backend/entity/README.md) | 15 个与 PostgreSQL 表对应的 DO，覆盖 139 个字段及其主键。 |
| [api](src/main/java/io/trove/backend/api/README.md) | 钱包登录与仓库权限的请求、响应和错误定义。 |
| `config` | Spring Security、前端 Origin 白名单和 MyBatis 类型配置。 |

当前已实现 Phantom 一次性签名登录、会话查询、退出和仓库权限核验。寄存、市场与履约接口将在后续阶段接入。

## WSL 构建

在 `backend/` 目录运行，Maven Wrapper 使用 Maven 3.9.15。构建需要 OpenJDK 25，无需数据库连接。

```bash
java -version
./mvnw clean package
```

生成的可执行 JAR 位于 `target/trove-backend-0.1.0-SNAPSHOT.jar`。

## 本地启动

在 PostgreSQL 准备独立的 Trove 数据库和用户，然后在启动进程的环境中配置以下变量。WSL 的后端需要填写它可以访问的数据库地址。

| 环境变量 | 用途 |
| --- | --- |
| `TROVE_DB_URL` | PostgreSQL JDBC URL，例如 `jdbc:postgresql://localhost:5432/trove`。 |
| `TROVE_DB_USER` | 数据库用户名。 |
| `TROVE_DB_PASSWORD` | 数据库密码。 |
| `TROVE_HTTP_HOST` | HTTP 监听地址，默认为 `127.0.0.1`。 |
| `TROVE_HTTP_PORT` | HTTP 端口，默认为 `8080`。 |
| `TROVE_ALLOWED_FRONTEND_ORIGINS` | 逗号分隔的前端 `scheme://host[:port]`，默认为 `http://localhost:5173,http://127.0.0.1:5173`。以实际浏览器 Origin 为准，不带末尾斜杠。 |

手工应用 `docs/backend/schema.sql` 的本地数据库已有业务表。本地联调使用以下方式启动：

```bash
SPRING_FLYWAY_ENABLED=false ./mvnw spring-boot:run
```

如果 WSL 内能访问后端，而 Windows Chrome 访问 `127.0.0.1:8080` 失败，可以在后端启动进程中设置 `JAVA_TOOL_OPTIONS=-Djava.net.preferIPv4Stack=true`，让 WSL 的 localhost 转发识别 IPv4 监听。

Swagger UI 在 `/swagger-ui.html`，OpenAPI JSON 在 `/v3/api-docs`。钱包功能使用 `wallet_account`、`wallet_login_challenge` 和 `wallet_account_role` 三张现有表。初次准备空库时可参照 [schema.sql](../docs/backend/schema.sql)。正式接入 Flyway 时，迁移脚本放在 `src/main/resources/db/migration/`。

## 钱包认证与工作台权限

1. 前端把当前钱包地址和网络发给 `POST /api/v1/auth/wallet/challenges`，后端校验浏览器 `Origin`，生成有效期 5 分钟的消息。数据库保存 nonce 摘要。
2. Phantom 签名后，前端把原挑战、地址、网络、nonce 和 Base64 签名提交到 `POST /api/v1/auth/wallet/sessions`。后端验证 Ed25519 签名并原子消费挑战，再发放 30 分钟的 Bearer 会话。
3. 客户端将 token 保存在页面内存，用 `Authorization: Bearer <token>` 查询 `GET /api/v1/auth/wallet/sessions/current`，退出时调用同一路径的 `DELETE`。后端会话存于单实例进程内，应用重启后需重新签名。
4. 访问 `GET /api/v1/warehouse/access` 时，后端查询该会话钱包在 `wallet_account_role` 的当前角色。会话缺失、失效或撤销返回 `401`，普通钱包返回 `403`，拥有 `WAREHOUSE_OPERATOR` 的钱包得到当前地址、网络和角色。后续仓库业务沿用同一角色检查。

本地仓管角色由运维在数据库预先绑定给对应的公钥。实际登录通过 Phantom 签名验证公钥归属，初始化角色记录时验证时间保持未登记。跨域联调时，挑战、签名验证和会话访问使用同一个前端 Origin。

项目概况见 [Trove](../README.md)。

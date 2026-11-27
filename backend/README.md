# Trove 后端

Spring Boot 4.2、OpenJDK 25、PostgreSQL、MyBatis-Plus、Flyway、Spring Security 和 springdoc OpenAPI。后端采用单应用 Maven 构建，默认入口为 `io.trove.backend.TroveApplication`。

## 项目结构

后端以 `src/main/java/io/trove/backend/` 为基础包，使用 `Controller → Service → Mapper → PostgreSQL` 的依赖方向。

| 包 | 内容 |
| --- | --- |
| [controller](src/main/java/io/trove/backend/controller/README.md) | 业务 HTTP 路由、入参绑定和正常响应。 |
| [advice](src/main/java/io/trove/backend/advice/README.md) | 按控制器范围把业务异常和请求绑定失败转换成 HTTP 错误响应。 |
| [service](src/main/java/io/trove/backend/service/README.md) 和 [service.impl](src/main/java/io/trove/backend/service/impl/README.md) | 钱包会话、角色查询、寄存状态流转与实物档案写入。 |
| [mapper](src/main/java/io/trove/backend/mapper/README.md) | 15 个 MyBatis-Plus Mapper 接口，已接入钱包身份、挑战、角色、寄存和在库实物。 |
| [entity](src/main/java/io/trove/backend/entity/README.md) | 15 个与 PostgreSQL 表对应的 DO，覆盖 139 个字段及其主键。 |
| [api](src/main/java/io/trove/backend/api/README.md) | 钱包登录、仓库权限与寄存的请求、响应和错误定义。 |
| `config` | Spring Security、前端 Origin 白名单、MyBatis 类型映射和 PostgreSQL 分页。 |

当前提供 Phantom 登录、寄存验收、市场与收藏查询、兑换草稿、履约进度和已索引交易回执。实物验收后保留 `itemId`。实际铸造、成交与兑换销毁将在相应的链上程序和最终确认索引接通后写入业务状态。

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
| `TROVE_SOLANA_DEVNET_RPC_URL` | 服务端 Devnet RPC，默认 `https://api.devnet.solana.com`。 |
| `TROVE_SOLANA_LOCALNET_RPC_URL` | 服务端 Localnet RPC，默认 `http://127.0.0.1:8899`。 |
| `TROVE_REDEMPTION_RECIPIENT_KEY_BASE64` | 首版收件资料的 Base64 编码 32 字节 AES 密钥，保存在运行环境。缺少有效密钥时兑换草稿写入和收件资料读取返回 `503`。 |
| `TROVE_PRIVATE_PHOTO_DIR` | WSL 中服务用户专用的绝对目录，权限 `0700`，位于任何静态文件服务目录之外。缺少有效目录时照片上传和文件下载返回 `503`。 |

新数据库首次启动时，Flyway 执行 `src/main/resources/db/migration/V1__trove_schema.sql` 创建基线表。已手工应用 `docs/backend/schema.sql` 的本地库继续使用现有表和以下联调命令。

```bash
SPRING_FLYWAY_ENABLED=false ./mvnw spring-boot:run
```

如果 WSL 内能访问后端，而 Windows Chrome 访问 `127.0.0.1:8080` 失败，可以在后端启动进程中设置 `JAVA_TOOL_OPTIONS=-Djava.net.preferIPv4Stack=true`，让 WSL 的 localhost 转发识别 IPv4 监听。

Swagger UI 在 `/swagger-ui.html`，OpenAPI JSON 在 `/v3/api-docs`。钱包功能使用 `wallet_account`、`wallet_login_challenge` 和 `wallet_account_role` 三张现有表。建表设计见 [schema.sql](../docs/backend/schema.sql)。新增数据库结构采用 `src/main/resources/db/migration/` 中的后续版本。

## 钱包认证与工作台权限

1. 前端把当前钱包地址和网络发给 `POST /api/v1/auth/wallet/challenges`，后端校验浏览器 `Origin`，生成有效期 5 分钟的消息。数据库保存 nonce 摘要。
2. Phantom 签名后，前端把原挑战、地址、网络、nonce 和 Base64 签名提交到 `POST /api/v1/auth/wallet/sessions`。后端验证 Ed25519 签名并原子消费挑战，再发放 30 分钟的 Bearer 会话。
3. 客户端将 token 保存在页面内存，用 `Authorization: Bearer <token>` 查询 `GET /api/v1/auth/wallet/sessions/current`，退出时调用同一路径的 `DELETE`。后端会话存于单实例进程内，应用重启后需重新签名。
4. 访问 `GET /api/v1/warehouse/access` 时，后端查询该会话钱包在 `wallet_account_role` 的当前角色。会话缺失、失效或撤销返回 `401`，普通钱包返回 `403`，拥有 `WAREHOUSE_OPERATOR` 的钱包得到当前地址、网络和角色。后续仓库业务沿用同一角色检查。

本地仓管角色由运维在数据库预先绑定给对应的公钥。实际登录通过 Phantom 签名验证公钥归属，初始化角色记录时验证时间保持未登记。跨域联调时，挑战、签名验证和会话访问使用同一个前端 Origin。

## 寄存与仓库验收

以下路径以 `/api/v1` 为前缀，调用时发送 `Authorization: Bearer <token>`。仓库路径还要求 `WAREHOUSE_OPERATOR` 角色。

| 接口 | 用途 |
| --- | --- |
| `POST /consignments` | 提交寄存申请。新建返回 `201`，相同请求重试返回原单及 `200`。 |
| `GET /consignments/me`、`GET /consignments/{id}` | 查看本人分页列表和寄存单详情。 |
| `GET /warehouse/consignments`、`GET /warehouse/consignments/{id}` | 查看仓库分页队列和寄存单详情。 |
| `POST /warehouse/consignments/{id}/receive` | 登记收货。 |
| `POST /warehouse/consignments/{id}/approve` | 提交 `displayTitle`、`verifiedCondition` 和 `warehouseLocation`，创建在库实物并返回 `itemId`。 |
| `POST /warehouse/consignments/{id}/reject` | 提交 `rejectionReason`，登记拒收。 |
| `POST /warehouse/consignments/{id}/return` | 登记退回。可提供 JSON 请求体和 `returnTrackingNumber`。 |

提交时由客户端生成 `clientRequestId`（UUID），并提供 `category`（`CARD` 或 `TOY`）、`submittedTitle`、`submittedCondition`、`identityKind`（`GRADING_CERTIFICATE` 或 `ITEM_SERIAL`）、`identityIssuer`、`identityNumber`。`inboundCarrier` 和 `inboundTrackingNumber` 可选。会话决定寄存人。重试应沿用同一个请求 UUID 和相同资料。

列表可传 `status`、`page` 和 `size`，默认第 1 页、每页 20 条，单页上限为 100 条。详情还会返回进度时间、拒收原因、运单和已生成的 `itemId`。访问其他钱包的单据返回 `404`，重复实物、已占用请求编号或错误状态流转返回 `409`。验收通过时状态为 `APPROVED_PENDING_MINT`，返回的 `itemId` 用于后续授权铸造。

### 私有寄存照片

申请人可在「待寄入」阶段向 `POST /consignments/{id}/photos` 发送原始 JPEG 或 PNG 图片。使用对应的 `Content-Type`，每张最大 5 MiB，每单最多八张。响应提供图片 UUID、MIME、展示顺序和上传时间。列表与下载使用 `GET /consignments/{id}/photos` 和 `GET /consignments/{id}/photos/{photoId}`。仓管员可通过 `/warehouse/consignments/{id}/photos` 下的对应 GET 路径读取。前端经 Bearer 请求获取图片。

## 市场、收藏、交易回执与兑换履约

| 接口 | 用途 |
| --- | --- |
| `GET /market/listings` | 按 `network`、可选 `category`（`CARD`、`TOY`）、`sort`（`NEWEST`、`PRICE_ASC`、`PRICE_DESC`）、`page` 和 `size` 查有效挂单。 |
| `GET /items/{itemId}`、`GET /items/{itemId}/sales` | 按 `network` 查公开藏品及已确认成交历史。金额和链上序号以十进制字符串返回。 |
| `GET /collection/me` | 钱包登录后查询。钱包直接持有取 Solana 最终确认槽，挂单托管取后端的有效挂单投影。RPC 不可用时返回 `503`。 |
| `GET /chain/transactions/{signature}` | 查询当前会话网络中与钱包相关且已索引的最终确认交易事件。未索引、未知或无权访问时统一返回 `404`。 |
| `POST /items/{itemId}/redemption-drafts` | 当前持有人提交 `recipientName`、`recipientPhone` 和 `recipientAddress`，领取或更新待签名草稿。响应只包含草稿信息。 |
| `GET /redemptions/me` | 查看当前钱包在该网络的兑换草稿和履约进度。 |
| `GET /warehouse/shipments` | 库管员分页查询由已确认兑换凭证生成的发货任务，可按 `status` 筛选。 |
| `GET /warehouse/shipments/{id}/recipient` | 库管员按单查看当前网络、未签收任务的姓名、电话和地址。响应禁止缓存。 |
| `POST /warehouse/shipments/{id}/events` | 库管员登记发货、签收、异常或异常解决。重试时沿用 `eventKey`，同键不同操作返回 `409`。 |

市场、藏品和成交列表可匿名读取。收藏、回执、草稿和本人进度要求钱包会话。仓库操作要求库管角色。收件资料经认证加密，收件人姓名、电话及地址仅在授权的单笔库管响应中返回。私有运单仅由对应兑换人和库管员查看。

项目概况见 [Trove](../README.md)。

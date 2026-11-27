# Trove 后端

Spring Boot 4.2、OpenJDK 25、PostgreSQL、MyBatis-Plus、Flyway、Spring Security 和 springdoc OpenAPI。后端采用单应用 Maven 构建，默认入口为 `io.trove.backend.TroveApplication`。

## 项目结构

后端以 `src/main/java/io/trove/backend/` 为基础包，使用 `Controller → Service → Mapper → PostgreSQL` 的依赖方向。

| 包 | 内容 |
| --- | --- |
| [controller](src/main/java/io/trove/backend/controller/README.md) | 8 个业务入口控制器，对应钱包认证、寄存、仓库、市场、藏品详情、收藏、兑换和链上交易查询。 |
| [service](src/main/java/io/trove/backend/service/README.md) 和 [service.impl](src/main/java/io/trove/backend/service/impl/README.md) | 15 张表对应的服务接口与实现骨架。 |
| [mapper](src/main/java/io/trove/backend/mapper/README.md) | 15 个 MyBatis-Plus Mapper 接口骨架。 |
| [entity](src/main/java/io/trove/backend/entity/README.md) | 15 个与 PostgreSQL 表对应的 DO 声明。 |
| `config` | Spring Security 等应用配置。 |

当前阶段建立包结构和类型继承。Controller 声明了路由前缀，业务接口待实现。DO 完成字段和主键映射后，再注册 Mapper 和 Service。

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

```bash
./mvnw spring-boot:run
```

Swagger UI 在 `/swagger-ui.html`，OpenAPI JSON 在 `/v3/api-docs`。骨架只开放文档路由，后续业务接口按需要配置钱包身份与角色权限。正式接入 Flyway 时，迁移脚本放在 `src/main/resources/db/migration/`。

项目概况见 [Trove](../README.md)。

# HTTP 异常处理

本包存放 `@RestControllerAdvice` 和 `@ExceptionHandler`。每个 `XxxErrorAdvice` 通过 `assignableTypes` 指定适用的控制器，把对应业务异常和已约定的请求格式错误转换为 HTTP 状态码与响应体。照片和收件资料等私有接口的错误缓存策略也由对应 Advice 处理。

- [controller](../controller/README.md) 接收请求、调用服务并返回正常响应。
- [api](../api/README.md) 保存请求、响应和业务错误类型。
- Spring Security 过滤链在控制器分发前处理会话与角色拦截，规则位于 [WebSecurityConfig](../config/WebSecurityConfig.java)。

从 [后端模块](../../../../../../../README.md) 运行 `./mvnw clean package` 验证构建。接口联调时检查格式错误和业务错误的 HTTP 状态、JSON 错误码与缓存响应头。

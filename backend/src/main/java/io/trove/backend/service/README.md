# 业务服务层

15 个 `XxxService` 接口继承 MyBatis-Plus 的 `IService<XxxDO>`，[实现类](impl/README.md)放在 `service.impl`。依赖方向是 Controller → Service → Mapper。按业务落地时，Controller 通过构造器注入所需的服务接口，Service 负责组织业务校验、跨表操作和事务。

持久化字段映射和主键策略完成后，再注册可注入的服务。复合主键表应通过包含全部键列的条件定位行。`getById`、`updateById` 等单 ID 方法适用于已经配置单列主键的持久化对象。

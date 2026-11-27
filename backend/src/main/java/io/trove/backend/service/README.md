# 业务服务层

15 个 `XxxService` 接口继承 MyBatis-Plus 的 `IService<XxxDO>`，[实现类](impl/README.md)放在 `service.impl`。依赖方向是 Controller → Service → Mapper。按业务落地时，Controller 通过构造器注入所需的服务接口，Service 负责组织业务校验、跨表操作和事务。

`WalletAuthService` 管理一次性签名挑战和消费事务。`WalletSessionService` 管理短期 Bearer 会话，并使用已注册的 `WalletAccountRoleService` 读取请求钱包的当前角色。

`ConsignmentService` 受理和查询寄存、登记仓库收货、验收及退件。本人查询按会话钱包限定数据，仓库查询由 Controller 的仓库角色保护。验收通过时同一事务登记实物编号，仓库拒收后完成退件即可释放申报编号。

`MarketplaceQueryService` 查询公开市场和本人收藏。`FinalizedTokenOwnershipService` 提供最终确认持仓。`ChainEventService` 按钱包和网络限制交易事件回执。

`RedemptionDraftCreationService` 校验当前持有人资格并保存加密草稿。`RedemptionDraftService` 查询本人进度。`ShipmentService` 维护发货队列及幂等状态转换。

持久化字段映射和主键策略完成后，再注册可注入的服务。复合主键表应通过包含全部键列的条件定位行。`getById`、`updateById` 等单 ID 方法适用于已经配置单列主键的持久化对象。

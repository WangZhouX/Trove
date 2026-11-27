# 数据访问层

本包的 15 个 `XxxMapper` 接口与现有 PostgreSQL 表一一对应，并继承 MyBatis-Plus 的 `BaseMapper<XxxDO>`。例如 [ChainEventMapper](ChainEventMapper.java) 对应 [ChainEventDO](../entity/ChainEventDO.java)。

当前是类型骨架。完成字段映射和主键策略后，再使用 `@Mapper` 注册。查询统一使用 MyBatis-Plus Lambda API。按复合主键查询、更新或删除单行时，Lambda 条件须匹配全部键列。例如 `chain_event` 需要同时匹配 `network`、`tx_signature` 和 `event_index`。事务边界由[服务层](../service/README.md)负责。

# PostgreSQL 类型处理

本目录集中维护持久化字段的 JDBC 类型转换。实体用字段级映射引用处理器。

`JsonbStringTypeHandler` 用于 `chain_event.payload` 的 `jsonb` 映射。写入以 JDBC `OTHER` 传递 JSON 字符串，由 PostgreSQL 根据目标列推断类型并检查约束。读取以字符串接收 PostgreSQL 返回的 JSON 文本。

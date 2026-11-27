# 持久化实体

此目录集中存放 Trove 的 PostgreSQL 行模型。每张表对应一个以 `DO` 结尾的 Java record，使用 `@TableName` 声明目标表。Mapper 使用这些类型读写数据，HTTP 接口使用独立的请求和响应类型。

15 个 record 按 `docs/backend/schema.sql` 映射 139 个字段，每个参数的 Javadoc 说明了中英文含义。`uuid` 对应 `UUID`，`timestamptz` 对应 `OffsetDateTime`，链上 `numeric(20,0)` 对应 `BigInteger`，`bytea` 对应 `byte[]`。整数列按可空性选择基本类型或包装类型。`jsonb` 映射为 `String`，使用 [字段类型处理器](typehandler/README.md) 绑定 PostgreSQL 参数。

MyBatis 为 PostgreSQL UUID 注册 JDBC 原生读写处理器，并按列名关联 record 的构造参数。查询返回 DO 时需包含完整字段。单独的列投影使用与之对应的 DTO。

单列主键用 `@TableId` 和 `IdType.INPUT` 由调用方提供。复合主键的每列使用 `@TableField` 声明，更新实体时保持键值。单行查询、更新和删除使用全部键列。主键提供 `getXxx()` 供 MyBatis-Plus Lambda 引用。涉及网络的表使用 `network` 区分 Localnet 与 Devnet。

| 表名 | Java 类型 | 主键列 |
| --- | --- | --- |
| `wallet_account` | `WalletAccountDO` | `wallet_address` |
| `wallet_login_challenge` | `WalletLoginChallengeDO` | `challenge_id` |
| `wallet_account_role` | `WalletAccountRoleDO` | `wallet_address, role_code` |
| `consignment` | `ConsignmentDO` | `consignment_id` |
| `consignment_photo` | `ConsignmentPhotoDO` | `photo_id` |
| `custody_item` | `CustodyItemDO` | `item_id` |
| `chain_event` | `ChainEventDO` | `network, tx_signature, event_index` |
| `chain_sync_cursor` | `ChainSyncCursorDO` | `network, program_id` |
| `chain_asset` | `ChainAssetDO` | `network, item_id` |
| `market_listing` | `MarketListingDO` | `network, item_id, listing_no` |
| `market_sale` | `MarketSaleDO` | `network, item_id, listing_no` |
| `redemption_draft` | `RedemptionDraftDO` | `draft_id` |
| `redemption_receipt` | `RedemptionReceiptDO` | `network, item_id` |
| `shipment` | `ShipmentDO` | `shipment_id` |
| `shipment_event` | `ShipmentEventDO` | `shipment_id, event_key` |

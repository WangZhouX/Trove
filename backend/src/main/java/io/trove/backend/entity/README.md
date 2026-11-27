# 持久化实体

此目录集中存放 Trove 的 PostgreSQL 行模型。每张表对应一个以 `DO` 结尾的 Java record，使用 `@TableName` 声明目标表。Mapper 使用这些类型读写数据，HTTP 接口使用独立的请求和响应类型。

当前实现范围为类型和表名映射。后续按 `docs/backend/erd.md` 与 `docs/backend/schema.sql` 补字段、列映射及中英文参数注释。`uuid` 对应 `UUID`，`timestamptz` 对应 `OffsetDateTime`，`numeric(20,0)` 的链上整数保留完整精度，可用 `BigInteger`，`bytea` 对应 `byte[]`。接入 `jsonb` 时明确 PostgreSQL 类型处理器及读写策略。

单列主键按表定义映射。复合主键包含下表列出的全部字段，单行查询、更新和删除通过完整键筛选。涉及网络的表使用 `network` 区分 Localnet 与 Devnet。

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

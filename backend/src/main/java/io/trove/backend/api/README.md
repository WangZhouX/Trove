# API 类型 / API Types

HTTP 请求、响应和业务错误类型放在此处。持久化实体由 `entity/` 管理。
This package holds HTTP requests, responses and business errors. Persistence records live in `entity/`.

`WalletAuthApi` 对应连接 Phantom、查询当前登录状态与进入仓库工作台的调用。
`WalletAuthApi` covers Phantom sign-in, the current session, and Warehouse Workbench access.

`ConsignmentApi` 对应寄存申请、本人进度查询和库管员处理。`ConsignmentException` 定义可供页面区分的错误码。
`ConsignmentApi` covers applications, depositor progress, and warehouse operations. `ConsignmentException` defines error codes the UI can distinguish.

`MarketApi` 定义市场、藏品、成交和收藏的公开字段。 `MarketApi` defines the fields for listings, items, sales, and collections.

`ChainTransactionApi` 只返回当前钱包可见的已索引事件摘要。 `ChainTransactionApi` returns indexed event summaries visible to the current wallet. `ChainTransactionException` 定义交易回执的查询错误。 `ChainTransactionException` defines receipt lookup errors.

`RedemptionDraftCreationApi` 定义私有兑换草稿请求与安全响应，`FulfillmentApi` 定义兑换进度、仓库发货队列和事件登记。 `RedemptionDraftCreationApi` defines private draft submissions and safe responses. `FulfillmentApi` defines redemption progress, the shipment queue, and event registration.

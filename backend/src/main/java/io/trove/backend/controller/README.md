# Controller 层

控制器按页面业务组织，负责接收 HTTP 请求、绑定入参和组织正常响应。业务处理交给 Service，异常响应由 [advice](../advice/README.md) 处理。

| 控制器 | 路由前缀 | 前端使用场景 |
| --- | --- | --- |
| `WalletAuthController` | `/api/v1/auth/wallet` | 已接入。连接 Phantom 后申请挑战并签名，支持查询会话和退出。 |
| `ConsignmentController` | `/api/v1/consignments` | 「寄存」提交申请。「我的寄存」按会话钱包查询列表和详情。 |
| `ConsignmentPhotoController` | `/api/v1/consignments/{id}/photos` 及仓库对应路径 | 申请人补传、下载私有照片，仓管员在验收时读取。 |
| `WarehouseController` | `/api/v1/warehouse` | 核验「仓库工作台」权限。在「入库与验真」查询队列、登记收货、记录验收通过或驳回、登记退回。 |
| `MarketController` | `/api/v1/market` | 「市场」首次加载及切换筛选条件。 |
| `ItemController` | `/api/v1/items` | 藏品详情页打开时查看公开信息、挂单和成交历史。 |
| `CollectionController` | `/api/v1/collection` | 「我的收藏」在钱包连接后加载持有和托管藏品。 |
| `RedemptionDraftController` | `/api/v1/items` | 持有人从「我的收藏」提交兑换的收件资料，返回待签名的草稿。 |
| `RedemptionController` | `/api/v1` | 「兑换进度」查询本人草稿、已确认凭证和履约进度。 |
| `WarehouseShipmentController` | `/api/v1/warehouse/shipments` | 仓管员查看发货任务、登记发货、签收或异常。 |
| `WarehouseShipmentRecipientController` | `/api/v1/warehouse/shipments/{shipmentId}/recipient` | 仓管员按单核对未签收任务的私有收件资料。 |
| `ChainTransactionController` | `/api/v1/chain/transactions` | 钱包查询已索引且与自身相关的交易事件回执。 |

新增业务接口时，在 OpenAPI 写中文名称，在方法 Javadoc 中写明页面、触发时机、调用条件和响应字段。

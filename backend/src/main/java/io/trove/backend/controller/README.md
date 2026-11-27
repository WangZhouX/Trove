# Controller 层

控制器按页面使用的业务组织，负责接收 HTTP 请求、校验入参与组织响应。业务处理交给 Service。

| 控制器 | 路由前缀 | 前端使用场景 |
| --- | --- | --- |
| `WalletAuthController` | `/api/v1/auth/wallet` | 连接 Phantom 或切换钱包后申请登录挑战并建立会话。 |
| `ConsignmentController` | `/api/v1/consignments` | 「寄存」提交申请、查看我的申请及验收进度。 |
| `WarehouseController` | `/api/v1/warehouse` | 「仓库工作台」处理收货、验收、铸造与发货任务。 |
| `MarketController` | `/api/v1/market` | 「市场」首次加载及切换筛选条件。 |
| `ItemController` | `/api/v1/items` | 藏品详情页打开时查看公开信息、挂单和成交历史。 |
| `CollectionController` | `/api/v1/collection` | 「我的收藏」在钱包连接后加载持有和托管藏品。 |
| `RedemptionController` | `/api/v1` | 提交兑换表单、准备签名及查看「兑换进度」。 |
| `ChainTransactionController` | `/api/v1/chain/transactions` | 签署铸造、市场或兑换交易后查询确认状态。 |

当前类只声明路由前缀和 OpenAPI 中文分组。兑换的前缀覆盖藏品下的兑换草稿、草稿签名和用户兑换查询，具体路径按后端接口设计定义。新增业务接口时，在方法 Javadoc 中写明页面、触发时机、调用条件和响应字段。

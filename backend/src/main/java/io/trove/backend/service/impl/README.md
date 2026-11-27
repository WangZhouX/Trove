# 服务实现层

表级 `XxxServiceImpl` 继承 MyBatis-Plus 的 `ServiceImpl<XxxMapper, XxxDO>` 并实现同名[服务接口](../README.md)。[ConsignmentServiceImpl](ConsignmentServiceImpl.java) 使用 MyBatis-Plus 分页查询，按状态推进寄存单，验收通过时在同一事务创建在库实物。

[WalletAuthServiceImpl](WalletAuthServiceImpl.java) 在同一事务中消费挑战并登记签名验证时间。[WalletSessionServiceImpl](WalletSessionServiceImpl.java) 在签名验证提交后发放进程内会话，调用注册后的 [WalletAccountRoleServiceImpl](WalletAccountRoleServiceImpl.java) 解析当前角色。后续业务按需注册相应表级服务。

[MarketplaceQueryServiceImpl](MarketplaceQueryServiceImpl.java) 关联实物、链上资产和挂单投影。 [FinalizedTokenOwnershipServiceImpl](FinalizedTokenOwnershipServiceImpl.java) 从会话网络的 Solana 节点核实直接持有状态。 [ChainEventServiceImpl](ChainEventServiceImpl.java) 返回经关联关系校验的回执摘要。

[RedemptionDraftCreationServiceImpl](RedemptionDraftCreationServiceImpl.java) 对草稿进行资格校验、加密和事务处理。 [ShipmentServiceImpl](ShipmentServiceImpl.java) 在同一事务登记发货、更新库存与记录履约事件。

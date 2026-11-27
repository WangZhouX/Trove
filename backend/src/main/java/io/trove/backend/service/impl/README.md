# 服务实现层

表级 `XxxServiceImpl` 继承 MyBatis-Plus 的 `ServiceImpl<XxxMapper, XxxDO>` 并实现同名[服务接口](../README.md)。例如 [ConsignmentServiceImpl](ConsignmentServiceImpl.java) 通过对应 Mapper 连接寄存数据的访问层。

[WalletAuthServiceImpl](WalletAuthServiceImpl.java) 在同一事务中消费挑战并登记签名验证时间。[WalletSessionServiceImpl](WalletSessionServiceImpl.java) 在签名验证提交后发放进程内会话，调用注册后的 [WalletAccountRoleServiceImpl](WalletAccountRoleServiceImpl.java) 解析当前角色。其余表级服务保留类型结构，业务接入时再注册。

# 服务实现层

`XxxServiceImpl` 继承 MyBatis-Plus 的 `ServiceImpl<XxxMapper, XxxDO>` 并实现同名[服务接口](../README.md)。例如 [ConsignmentServiceImpl](ConsignmentServiceImpl.java) 通过对应 Mapper 连接寄存数据的访问层。

当前类只建立类型关系。字段映射与 Mapper 注册完成后，再通过 `@Service` 纳入 Spring 容器并实现业务方法。跨表写入的事务边界置于对应的业务服务方法。

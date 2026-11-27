package io.trove.backend.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.autoconfigure.ConfigurationCustomizer;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.apache.ibatis.type.ObjectTypeHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;

/**
 * 配置 Trove 持久化实体的 JDBC 类型映射和 PostgreSQL 分页。
 * Configures JDBC type mappings and PostgreSQL pagination for Trove persistence.
 */
@Configuration(proxyBeanMethods = false)
class MybatisMappingConfig {

    /**
     * 注册 PostgreSQL 分页，供 Mapper 分页查询生成数据库级边界和总数查询。
     * Registers PostgreSQL pagination so mapper page queries apply database bounds and count rows.
     *
     * @return 包含 PostgreSQL 分页插件的 MyBatis-Plus 拦截器。 The MyBatis-Plus interceptor with PostgreSQL pagination.
     */
    @Bean
    MybatisPlusInterceptor mybatisPlusInterceptor() {
        var interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.POSTGRE_SQL));
        return interceptor;
    }

    /**
     * 注册 PostgreSQL UUID 映射，供 MyBatis 绑定写入参数和读取实体。
     * Registers PostgreSQL UUID mapping for MyBatis write parameters and record reads.
     *
     * @return 仅配置 UUID 的 MyBatis 定制器。 A MyBatis customizer scoped to UUID.
     */
    @Bean
    ConfigurationCustomizer uuidTypeMapping() {
        // PostgreSQL JDBC 的 getObject 和 setObject 原生识别 UUID。
        // PostgreSQL JDBC reads and writes UUID natively through getObject and setObject.
        return configuration -> configuration.getTypeHandlerRegistry().register(UUID.class, ObjectTypeHandler.class);
    }
}

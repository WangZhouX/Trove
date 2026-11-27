package io.trove.backend.config;

import com.baomidou.mybatisplus.autoconfigure.ConfigurationCustomizer;
import org.apache.ibatis.type.ObjectTypeHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;

/**
 * 配置 Trove 持久化实体与 PostgreSQL JDBC 类型的映射。
 * Configures PostgreSQL JDBC mappings for Trove persistence records.
 */
@Configuration(proxyBeanMethods = false)
class MybatisMappingConfig {

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

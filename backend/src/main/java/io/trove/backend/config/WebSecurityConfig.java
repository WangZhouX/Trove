package io.trove.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 钱包身份接入前的 HTTP 访问规则。
 * HTTP access rules before wallet authentication is implemented.
 */
@Configuration(proxyBeanMethods = false)
class WebSecurityConfig {

    /**
     * 允许浏览 API 文档，待后续实现后按业务接口声明访问权限。
     * Allows access to API documentation and reserves application routes for explicit access rules.
     *
     * @param http Spring Security 的 HTTP 配置构建器 / Spring Security HTTP configuration builder
     * @return HTTP 安全过滤链 / HTTP security filter chain
     * @throws Exception 安全配置构建失败 / if the security chain cannot be built
     */
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http.authorizeHttpRequests(requests -> requests
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs", "/v3/api-docs/**")
                        .permitAll()
                        .anyRequest().denyAll())
                .build();
    }
}

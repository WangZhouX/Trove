package io.trove.backend.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.trove.backend.api.WalletAuthException;
import io.trove.backend.service.WalletAccountRoleService;
import io.trove.backend.service.WalletSessionService;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * HTTP 钱包认证、来源配置和仓库授权规则。
 * HTTP wallet authentication, origin policy and warehouse authorization.
 */
@Configuration(proxyBeanMethods = false)
@SecurityScheme(name = "walletBearer", type = SecuritySchemeType.HTTP, scheme = "bearer")
class WebSecurityConfig {

    /**
     * 开放公开登录端点，并为会话与仓库资源应用独立权限。
     * Exposes public sign-in endpoints and applies distinct access rules to sessions and warehouse resources.
     *
     * @param http Spring Security 的 HTTP 配置构建器 / Spring Security HTTP configuration builder
     * @param sessions 提供 Bearer 身份及当前角色的会话服务 / bearer session identity and current-role service
     * @param origins 前端登录和跨域请求共用的来源配置 / origin policy shared by sign-in and CORS
     * @param json JSON 错误响应使用的应用序列化器 / application JSON serializer for error responses
     * @return HTTP 安全过滤链 / HTTP security filter chain
     * @throws Exception 安全配置构建失败 / if the security chain cannot be built
     */
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, WalletSessionService sessions,
                                            WalletOriginPolicy origins, JsonMapper json) throws Exception {
        return http.cors(cors -> cors.configurationSource(corsConfiguration(origins)))
                // 所有受保护请求显式发送 Bearer 凭证，免用 Cookie 认证与浏览器请求缓存。
                // All protected requests send bearer credentials explicitly, obviating cookie auth and saved requests.
                .csrf(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, exception) -> {
                            response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
                            writeError(response, json, WalletAuthException.Error.WALLET_SIGN_IN_REQUIRED);
                        })
                        .accessDeniedHandler((request, response, exception) ->
                                writeError(response, json, WalletAuthException.Error.WAREHOUSE_ROLE_REQUIRED)))
                .authorizeHttpRequests(requests -> requests
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs", "/v3/api-docs/**")
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/wallet/challenges",
                                "/api/v1/auth/wallet/sessions").permitAll()
                        .requestMatchers("/api/v1/auth/wallet/sessions/current").authenticated()
                        .requestMatchers("/api/v1/warehouse", "/api/v1/warehouse/**")
                        .hasAuthority(WalletAccountRoleService.WAREHOUSE_OPERATOR)
                        .anyRequest().denyAll())
                .addFilterBefore(new WalletSessionFilter(sessions), AnonymousAuthenticationFilter.class)
                .build();
    }

    /**
     * 为浏览器从前端访问当前 API 预配精确 CORS 权限。 / Configures exact cross-origin permissions for the current API.
     *
     * @param origins 允许的浏览器来源 / allowed browser origins
     * @return 仅注册 API 路径上的跨域配置 / CORS configuration registered only for API routes
     */
    private static CorsConfigurationSource corsConfiguration(WalletOriginPolicy origins) {
        var cors = new CorsConfiguration();
        cors.setAllowedOrigins(origins.allowedOrigins());
        cors.setAllowedMethods(List.of("GET", "POST", "DELETE"));
        cors.setAllowedHeaders(List.of(HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE, HttpHeaders.ACCEPT));
        cors.setMaxAge(600L);
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/v1/**", cors);
        return source;
    }

    /**
     * 安全过滤器先于 Controller 执行，因此直接发送固定 JSON 错误。
     * Writes a fixed JSON response because authorization runs before controllers.
     *
     * @param response 将发送给客户端的响应 / response sent to the client
     * @param json 应用 JSON 序列化器 / application JSON serializer
     * @param error 认证或权限错误 / authentication or authorization error
     * @throws IOException 响应写入失败 / if the response cannot be written
     */
    private static void writeError(HttpServletResponse response, JsonMapper json, WalletAuthException.Error error)
            throws IOException {
        response.setStatus(error.status().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        json.writeValue(response.getOutputStream(), error.response());
    }
}

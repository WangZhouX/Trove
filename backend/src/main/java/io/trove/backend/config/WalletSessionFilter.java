package io.trove.backend.config;

import io.trove.backend.service.WalletSessionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/** 把 Bearer 会话转换成 Spring Security 认证。 / Resolves bearer sessions into Spring Security authentication. */
final class WalletSessionFilter extends OncePerRequestFilter {

    /** Bearer 会话与实时角色的查询入口。 / Bearer session and current-role lookup service. */
    private final WalletSessionService sessions;

    /**
     * 创建安全过滤链专用的钱包过滤器。 / Create the wallet filter for the security chain.
     *
     * @param sessions Bearer 会话服务 / bearer session service
     */
    WalletSessionFilter(WalletSessionService sessions) {
        this.sessions = sessions;
    }

    /**
     * 读取身份与权限并继续当前请求过滤链。 / Resolve identity and authorities and continue the current request.
     *
     * @param request HTTP 请求 / HTTP request
     * @param response HTTP 响应 / HTTP response
     * @param chain 后续过滤器 / remaining filters
     * @throws ServletException Servlet 处理失败 / if servlet processing fails
     * @throws IOException 读写请求或响应失败 / if request or response I/O fails
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        sessions.authenticate(request.getHeader(HttpHeaders.AUTHORIZATION), request.getHeader(HttpHeaders.ORIGIN))
                .ifPresent(identity -> {
                    // 只向安全上下文传递已验证身份和数据库角色，原 Bearer 凭证不进入认证对象。
                    // Pass only the authenticated identity and database roles into security, omitting the raw bearer token.
                    var authorities = identity.roles().stream().map(SimpleGrantedAuthority::new).toList();
                    var authentication = UsernamePasswordAuthenticationToken.authenticated(identity, null, authorities);
                    var context = SecurityContextHolder.createEmptyContext();
                    context.setAuthentication(authentication);
                    SecurityContextHolder.setContext(context);
                });
        chain.doFilter(request, response);
    }
}

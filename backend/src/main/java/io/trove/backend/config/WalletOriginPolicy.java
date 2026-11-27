package io.trove.backend.config;

import io.trove.backend.api.WalletAuthException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

/**
 * 为 CORS 和钱包登录共用一套精确来源配置。
 * Shares one strict frontend origin policy between CORS and wallet sign-in.
 */
@Component
public final class WalletOriginPolicy {

    /** 浏览器 Origin 的允许值，格式为 scheme://host[:port]。 / Exact browser origins formatted as scheme://host[:port]. */
    private final List<String> origins;

    /**
     * 启动时校验所有允许来源，供安全过滤链和登录服务使用。
     * Validates all allowed frontend origins at startup for use by the filter chain and sign-in service.
     *
     * @param configuredOrigins 逗号分隔的前端 Origin / comma-separated frontend origins
     * @throws IllegalArgumentException 配置含非法来源 / if any origin is invalid
     */
    public WalletOriginPolicy(@Value("${TROVE_ALLOWED_FRONTEND_ORIGINS:http://localhost:5173,http://127.0.0.1:5173}")
                              String configuredOrigins) {
        origins = Arrays.stream(configuredOrigins.split(",", -1)).map(String::trim).distinct().toList();
        if (origins.isEmpty() || origins.stream().anyMatch(origin -> !isHttpOrigin(origin))) {
            throw new IllegalArgumentException("TROVE_ALLOWED_FRONTEND_ORIGINS requires exact HTTP(S) origins");
        }
    }

    /** @return CORS 使用的允许来源列表 / allowed origins used for CORS */
    public List<String> allowedOrigins() {
        return origins;
    }

    /**
     * 验证登录请求的 Origin，服务端用它生成及校验签名消息。
     * Checks the request Origin used by the server to build and verify the sign-in message.
     *
     * @param origin 请求携带的浏览器 Origin / browser Origin supplied by the request
     * @throws WalletAuthException Origin 缺失或未获准 / if the origin is missing or disallowed
     */
    public void requireAllowed(String origin) {
        if (!origins.contains(origin == null ? "" : origin)) {
            throw new WalletAuthException(WalletAuthException.Error.ORIGIN_NOT_ALLOWED);
        }
    }

    /**
     * 判断配置项是否为浏览器使用的 HTTP(S) 来源。 / Checks if a setting is an HTTP(S) browser origin.
     *
     * @param origin 待检查的配置值 / origin configuration value
     * @return 是否为合法来源 / whether the origin is valid
     */
    private static boolean isHttpOrigin(String origin) {
        try {
            URI uri = URI.create(origin);
            return origin.length() <= 255 && ("http".equals(uri.getScheme()) || "https".equals(uri.getScheme()))
                    && uri.getHost() != null && uri.getUserInfo() == null
                    && (uri.getPort() == -1 || uri.getPort() > 0 && uri.getPort() <= 65535)
                    && (uri.getRawPath() == null || uri.getRawPath().isEmpty())
                    && uri.getRawQuery() == null && uri.getRawFragment() == null;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}

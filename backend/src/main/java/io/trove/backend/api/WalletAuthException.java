package io.trove.backend.api;

import org.springframework.http.HttpStatus;

/**
 * 可返回固定业务错误的认证异常。
 * Authentication exception that carries a fixed error suitable for HTTP clients.
 */
public final class WalletAuthException extends RuntimeException {

    /** 对外的固定错误，不携带签名、令牌和数据库信息。 / Public error without signatures, tokens or database details. */
    private final Error error;

    /**
     * 创建认证流程中的失败结果。 / Create a failure for the authentication flow.
     *
     * @param error 对外错误码、状态和说明 / public code, status and explanation
     */
    public WalletAuthException(Error error) {
        super(error.message());
        this.error = error;
    }

    /**
     * 返回可安全对外的错误。 / Return the error safe for public responses.
     *
     * @return 当前错误定义 / current error definition
     */
    public Error error() {
        return error;
    }

    /** 认证拒绝原因及对应 HTTP 状态。 / Authentication failure reasons and their HTTP statuses. */
    public enum Error {
        INVALID_REQUEST(HttpStatus.BAD_REQUEST, "钱包登录请求格式不正确。"),
        ORIGIN_NOT_ALLOWED(HttpStatus.FORBIDDEN, "当前站点未获准发起钱包登录。"),
        CHALLENGE_REJECTED(HttpStatus.UNAUTHORIZED, "钱包签名验证失败或挑战已失效，请重新登录。"),
        WALLET_SIGN_IN_REQUIRED(HttpStatus.UNAUTHORIZED, "请连接 Phantom 并签名登录。"),
        WAREHOUSE_ROLE_REQUIRED(HttpStatus.FORBIDDEN, "当前钱包没有仓库操作权限。");

        /** 由错误处理器写入的状态码和提示文字。 / Status and message written by the error handlers. */
        private final HttpStatus status;
        private final String message;

        Error(HttpStatus status, String message) {
            this.status = status;
            this.message = message;
        }

        /** @return HTTP 状态 / HTTP status */
        public HttpStatus status() {
            return status;
        }

        /** @return 页面可显示的提示文字 / message suitable for the UI */
        public String message() {
            return message;
        }

        /** @return HTTP 错误响应字段 / HTTP error response fields */
        public Response response() {
            return new Response(name(), message);
        }
    }

    /**
     * 固定的认证错误响应。 / Stable authentication error response.
     *
     * @param code 客户端可识别的错误码 / client-recognizable error code
     * @param message 页面可显示的说明 / safe message for the UI
     */
    public record Response(String code, String message) {
    }
}

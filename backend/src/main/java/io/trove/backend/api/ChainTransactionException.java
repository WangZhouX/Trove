package io.trove.backend.api;

import org.springframework.http.HttpStatus;

/**
 * 交易回执接口自身的稳定错误。 / Stable errors specific to the transaction receipt endpoint.
 */
public final class ChainTransactionException extends RuntimeException {

    /** 回执查询失败的固定类型、HTTP 状态和提示。 / Fixed receipt lookup failure type, HTTP status and feedback. */
    private final Error error;

    /**
     * 创建可安全返回给访问者的查询错误。 / Creates a lookup error safe to return to the caller.
     *
     * @param error 固定的回执查询错误 / fixed receipt lookup error
     */
    public ChainTransactionException(Error error) {
        super(error.message);
        this.error = error;
    }

    /** @return 对应的 HTTP 状态 / corresponding HTTP status */
    public HttpStatus status() {
        return error.status;
    }

    /** @return 稳定的公开错误响应 / stable public error response */
    public ChainTransactionApi.ErrorResponse response() {
        return new ChainTransactionApi.ErrorResponse(error.name(), error.message);
    }

    /** 公开回执查询错误分类。 / Public receipt lookup error types. */
    public enum Error {
        INVALID_TRANSACTION_SIGNATURE(HttpStatus.BAD_REQUEST, "交易签名格式不正确。"),
        CHAIN_TRANSACTION_NOT_AVAILABLE(HttpStatus.NOT_FOUND, "当前钱包无可查的已索引回执。");

        /** 对应的 HTTP 状态和页面提示。 / Corresponding HTTP status and client-facing feedback. */
        private final HttpStatus status;
        private final String message;

        /**
         * 定义安全返回的错误状态和提示。 / Defines status and feedback safe to return to the caller.
         *
         * @param status HTTP 状态 / HTTP status
         * @param message 可直接展示的提示 / displayable feedback
         */
        Error(HttpStatus status, String message) {
            this.status = status;
            this.message = message;
        }
    }
}

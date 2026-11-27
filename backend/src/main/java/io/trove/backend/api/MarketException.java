package io.trove.backend.api;

import org.springframework.http.HttpStatus;

/**
 * 向市场或收藏页面提供稳定的错误码与可展示的提示。
 * Provides stable error codes and displayable feedback to market and collection clients.
 */
public final class MarketException extends RuntimeException {

    /** 错误状态和公开提示。 / HTTP status and public error feedback. */
    private final Error error;

    /**
     * 生成市场、藏品或收藏接口的业务错误。 / Create a market, item or collection failure.
     *
     * @param error 已知的业务错误 / known business error
     */
    public MarketException(Error error) {
        super(error.message);
        this.error = error;
    }

    /** @return 业务错误的 HTTP 状态 / HTTP status for this error */
    public HttpStatus status() {
        return error.status;
    }

    /** @return 稳定的公开错误响应 / stable public error response */
    public Response response() {
        return new Response(error.name(), error.message);
    }

    /** 市场读取的业务错误分类。 / Error types for marketplace queries. */
    public enum Error {
        MARKET_INVALID_REQUEST(HttpStatus.BAD_REQUEST, "市场查询参数不正确。"),
        ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "在所选网络未找到该藏品。"),
        COLLECTION_OWNERSHIP_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "暂时无法核对链上持有情况，请稍后重试。");

        /** HTTP 状态和页面提示。 / HTTP status and UI-safe feedback. */
        private final HttpStatus status;
        private final String message;

        /**
         * 保存 HTTP 状态与页面错误提示。 / Store the HTTP status and client-facing feedback.
         *
         * @param status 对应的 HTTP 状态 / HTTP status mapped to this error
         * @param message 前端可展示的提示 / client-facing feedback
         */
        Error(HttpStatus status, String message) {
            this.status = status;
            this.message = message;
        }
    }

    /**
     * 市场查询的公开错误结构。 / Public marketplace query error payload.
     *
     * @param code 供前端判断的稳定错误码 / stable code for the client
     * @param message 可直接展示的用户提示 / user-facing feedback
     */
    public record Response(String code, String message) {
    }
}

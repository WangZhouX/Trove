package io.trove.backend.api;

import org.springframework.http.HttpStatus;

/**
 * 单笔仓库收件资料查询的脱敏业务错误。
 * Sanitized workflow errors for checking an individual shipment's delivery details.
 */
public final class WarehouseShipmentRecipientException extends RuntimeException {

    /** HTTP 状态和固定提示对应的错误类型。 / Failure type with its HTTP status and fixed feedback. */
    private final Error error;

    /**
     * 使用业务错误的固定提示构造异常，避免回传内部原因。
     * Uses fixed workflow feedback without exposing the internal cause.
     *
     * @param error 稳定的错误类型 / stable type of failure
     */
    public WarehouseShipmentRecipientException(Error error) {
        super(error.message);
        this.error = error;
    }

    /**
     * 取得错误处理所需的 HTTP 状态。 / Gets the HTTP status for the error handler.
     *
     * @return 对应的响应状态 / corresponding response status
     */
    public HttpStatus status() {
        return error.status;
    }

    /**
     * 生成只包含固定说明的 HTTP 响应体。 / Creates an HTTP payload containing only fixed feedback.
     *
     * @return 稳定的失败码和消息 / stable failure code and message
     */
    public Response response() {
        return new Response(error.name(), getMessage());
    }

    /** 收件资料查询错误类型。 / Types of failure when looking up delivery details. */
    public enum Error {
        INVALID_REQUEST(HttpStatus.BAD_REQUEST, "发货任务编号格式不正确。"),
        AUTHENTICATION_REQUIRED(HttpStatus.UNAUTHORIZED, "请先签名登录钱包。"),
        OPERATOR_REQUIRED(HttpStatus.FORBIDDEN, "当前钱包无仓库操作权限。"),
        SHIPMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "未找到可核对收件资料的发货任务。"),
        RECIPIENT_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "暂时无法读取该任务的收件资料。");

        /** HTTP 响应状态。 / HTTP response status. */
        private final HttpStatus status;

        /** 不含收件信息或解密原因的提示。 / Feedback without recipient details or decryption causes. */
        private final String message;

        /**
         * 定义对外响应状态和内容。 / Defines the external response status and feedback.
         *
         * @param status HTTP 状态 / HTTP status
         * @param message 固定的脱敏提示 / fixed sanitized feedback
         */
        Error(HttpStatus status, String message) {
            this.status = status;
            this.message = message;
        }
    }

    /**
     * 稳定的仓库履约错误响应。 / Stable error payload for warehouse fulfillment.
     *
     * @param code 客户端使用的错误码 / client-facing error code
     * @param message 可展示的固定提示 / fixed feedback suitable for display
     */
    public record Response(String code, String message) {
    }
}

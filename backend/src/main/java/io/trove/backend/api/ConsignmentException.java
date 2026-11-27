package io.trove.backend.api;

import org.springframework.http.HttpStatus;

/**
 * 寄存业务失败与可向页面显示的提示。
 * Consignment failures carrying messages suitable for display to the client.
 */
public final class ConsignmentException extends RuntimeException {

    /** 页面使用的业务错误类型。 / Business error type used by the client. */
    private final Error error;

    /**
     * 使用错误类型的默认提示。 / Use the default feedback for this error type.
     *
     * @param error 业务错误类型 / business error type
     */
    public ConsignmentException(Error error) {
        this(error, error.message());
    }

    /**
     * 使用服务层提供的安全提示。 / Use safe feedback supplied by the service.
     *
     * @param error 业务错误类型 / business error type
     * @param message 可对外显示的说明 / explanation suitable for the client
     */
    public ConsignmentException(Error error, String message) {
        super(message);
        this.error = error;
    }

    /** @return 业务错误类型 / business error type */
    public Error error() {
        return error;
    }

    /** @return 客户端错误响应 / client error response */
    public Response response() {
        return new Response(error.name(), getMessage());
    }

    /**
     * 稳定的寄存错误码和 HTTP 状态。 / Stable consignment error codes and HTTP statuses.
     */
    public enum Error {
        INVALID_REQUEST(HttpStatus.BAD_REQUEST, "寄存请求格式或字段不正确。"),
        NOT_FOUND(HttpStatus.NOT_FOUND, "未找到寄存单。"),
        REQUEST_ID_CONFLICT(HttpStatus.CONFLICT, "该请求编号已用于不同的寄存资料。"),
        PHYSICAL_IDENTITY_CONFLICT(HttpStatus.CONFLICT, "该实物已在寄存流程中或已完成入库。"),
        STATUS_CONFLICT(HttpStatus.CONFLICT, "当前寄存状态无法执行此操作，请刷新后重试。");

        /** 错误对应的 HTTP 状态和默认提示。 / HTTP status and default feedback for the error. */
        private final HttpStatus status;
        private final String message;

        /**
         * 定义对外返回的状态和提示。 / Define the status and feedback returned to the client.
         *
         * @param status HTTP 状态 / HTTP status
         * @param message 页面可展示的默认提示 / default feedback suitable for the UI
         */
        Error(HttpStatus status, String message) {
            this.status = status;
            this.message = message;
        }

        /** @return 对应的 HTTP 状态 / corresponding HTTP status */
        public HttpStatus status() {
            return status;
        }

        /** @return 默认提示 / default feedback */
        public String message() {
            return message;
        }
    }

    /**
     * 寄存 HTTP 错误结构。 / Consignment HTTP error payload.
     *
     * @param code 供程序判断的稳定错误码 / stable code for the client
     * @param message 可展示给当前用户的提示 / feedback suitable for the current user
     */
    public record Response(String code, String message) {
    }
}

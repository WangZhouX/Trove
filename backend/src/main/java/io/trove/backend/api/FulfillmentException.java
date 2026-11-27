package io.trove.backend.api;

import org.springframework.http.HttpStatus;

/** 兑换及履约的安全业务错误。 / Safe business failures for redemption and fulfillment. */
public final class FulfillmentException extends RuntimeException {

    /** 客户端可识别的错误类型。 / Client-recognizable error type. */
    private final Error error;

    /**
     * 以稳定错误码构造失败。 / Constructs a failure with a stable error code.
     *
     * @param error 安全错误分类 / safe error category
     */
    public FulfillmentException(Error error) {
        super(error.message);
        this.error = error;
    }

    /** @return 错误对应的 HTTP 状态 / HTTP status for this failure */
    public HttpStatus status() {
        return error.status;
    }

    /** @return 不含敏感请求字段的响应 / response without sensitive request fields */
    public Response response() {
        return new Response(error.name(), getMessage());
    }

    /** 错误码和页面反馈。 / Error code and client feedback. */
    public enum Error {
        INVALID_FULFILLMENT_REQUEST(HttpStatus.BAD_REQUEST, "兑换或履约请求格式不正确。"),
        WAREHOUSE_ROLE_REQUIRED(HttpStatus.FORBIDDEN, "此操作需要仓库角色。"),
        SHIPMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "未找到发货任务。"),
        SHIPMENT_EVENT_CONFLICT(HttpStatus.CONFLICT, "该事件键已用于其他履约操作。"),
        SHIPMENT_STATUS_CONFLICT(HttpStatus.CONFLICT, "当前履约状态无法执行该操作，请刷新后重试。"),
        CUSTODY_STATE_CONFLICT(HttpStatus.CONFLICT, "实物当前无法出库，请核对在库记录。");

        /** 错误的 HTTP 状态。 / HTTP status for this error. */
        private final HttpStatus status;

        /** 可返回给页面的中文提示。 / Chinese feedback suitable for the client. */
        private final String message;

        /**
         * 声明稳定的页面错误。 / Declares a stable client error.
         *
         * @param status HTTP 状态码 / HTTP status
         * @param message 不含隐私信息的提示 / feedback free of private data
         */
        Error(HttpStatus status, String message) {
            this.status = status;
            this.message = message;
        }
    }

    /**
     * 统一的履约错误响应。 / Stable fulfillment error response.
     *
     * @param code 客户端错误码 / client error code
     * @param message 可展示的提示 / feedback suitable for display
     */
    public record Response(String code, String message) {
    }
}

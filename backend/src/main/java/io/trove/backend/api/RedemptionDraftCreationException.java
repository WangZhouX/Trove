package io.trove.backend.api;

import org.springframework.http.HttpStatus;

/**
 * 保存私有兑换草稿时可安全返回的业务失败。
 * Business failures that may be safely returned when saving a private redemption draft.
 */
public final class RedemptionDraftCreationException extends RuntimeException {

    /** 失败类型与客户端 HTTP 状态。 / Failure type and its HTTP status. */
    private final Error error;

    /**
     * 仅用脱敏的默认提示描述失败。
     * Describes the failure using only its sanitized default feedback.
     *
     * @param error 稳定的客户端错误类型 / stable client error type
     */
    public RedemptionDraftCreationException(Error error) {
        super(error.message);
        this.error = error;
    }

    /**
     * 取得错误对应的 HTTP 状态。
     * Gets the HTTP status for this failure.
     *
     * @return 业务错误状态 / business error status
     */
    public HttpStatus status() {
        return error.status;
    }

    /**
     * 转换成不包含请求原文的响应。
     * Converts the failure to a response without request values.
     *
     * @return 稳定的错误码和提示 / stable error code and feedback
     */
    public Response response() {
        return new Response(error.name(), getMessage());
    }

    /** 兑换草稿写入错误。 / Redemption draft write errors. */
    public enum Error {
        INVALID_REQUEST(HttpStatus.BAD_REQUEST, "收件资料或请求格式不正确。"),
        AUTHENTICATION_REQUIRED(HttpStatus.UNAUTHORIZED, "请先签名登录钱包。"),
        ASSET_NOT_OWNED(HttpStatus.NOT_FOUND, "当前钱包未持有可兑换的藏品。"),
        ASSET_INELIGIBLE(HttpStatus.CONFLICT, "藏品正在出售、已兑换或当前不在库。"),
        DRAFT_SIGNING(HttpStatus.CONFLICT, "现有草稿正在签名或等待链上确认，暂时无法修改。"),
        CONCURRENT_UPDATE(HttpStatus.CONFLICT, "兑换草稿已更新，请重试。"),
        OWNERSHIP_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "暂时无法核实链上持有人，请稍后重试。"),
        RECIPIENT_PROTECTION_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "暂时无法安全保存收件资料。");

        /** 错误响应的 HTTP 状态。 / HTTP status of the error response. */
        private final HttpStatus status;

        /** 客户端可见的固定提示。 / Fixed feedback safe for the client. */
        private final String message;

        /**
         * 定义响应状态与脱敏提示。
         * Defines the response status and sanitized feedback.
         *
         * @param status HTTP 状态 / HTTP status
         * @param message 可对外展示的固定提示 / fixed public feedback
         */
        Error(HttpStatus status, String message) {
            this.status = status;
            this.message = message;
        }
    }

    /**
     * 兑换草稿接口错误响应。 / Redemption draft API error response.
     *
     * @param code 客户端使用的稳定错误码 / stable error code used by the client
     * @param message 不含收件资料的提示 / feedback without delivery details
     */
    public record Response(String code, String message) {
    }
}

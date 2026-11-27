package io.trove.backend.api;

import org.springframework.http.HttpStatus;

/** 私有照片的稳定业务错误。 / Stable private-photo workflow errors. */
public final class ConsignmentPhotoException extends RuntimeException {

    /** 页面可识别的业务失败。 / Workflow failure recognized by the UI. */
    private final Error error;

    /**
     * 构造不包含照片路径或原始请求数据的失败。 / Creates a failure without photo paths or raw request data.
     *
     * @param error 对外错误代码 / client-facing error code
     */
    public ConsignmentPhotoException(Error error) {
        super(error.message);
        this.error = error;
    }

    /** @return 对应 HTTP 状态 / corresponding HTTP status */
    public HttpStatus status() {
        return error.status;
    }

    /** @return 对外可用的安全错误结构 / safe public error representation */
    public Response response() {
        return new Response(error.name(), getMessage());
    }

    /** 错误码与可向当前用户显示的提示。 / Codes and feedback suitable for the current caller. */
    public enum Error {
        INVALID_PHOTO_REQUEST(HttpStatus.BAD_REQUEST, "照片请求格式不正确。"),
        PHOTO_NOT_FOUND(HttpStatus.NOT_FOUND, "未找到寄存单或照片。"),
        WAREHOUSE_ROLE_REQUIRED(HttpStatus.FORBIDDEN, "查看仓库照片需要库管员权限。"),
        PHOTO_UPLOAD_CLOSED(HttpStatus.CONFLICT, "寄存单当前无法补传照片。"),
        PHOTO_LIMIT_REACHED(HttpStatus.CONFLICT, "寄存单已达到照片上限。"),
        INVALID_PHOTO(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "请上传完整有效的 JPEG 或 PNG 照片。"),
        PHOTO_TOO_LARGE(HttpStatus.CONTENT_TOO_LARGE, "照片不得超过 5 MiB 或像素限制。"),
        PHOTO_STORAGE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "私有照片存储暂不可用。");

        /** 该业务失败的 HTTP 状态。 / HTTP status for the workflow failure. */
        private final HttpStatus status;

        /** 不含路径、文件名和原始资料的安全提示。 / Safe message without paths, filenames or submitted data. */
        private final String message;

        /**
         * 定义可安全返回的业务错误。 / Defines the safe public workflow failure.
         *
         * @param status 对应 HTTP 状态 / corresponding HTTP status
         * @param message 可向当前用户显示的提示 / feedback suitable for the current caller
         */
        Error(HttpStatus status, String message) {
            this.status = status;
            this.message = message;
        }
    }

    /**
     * 错误响应结构。 / Error response.
     *
     * @param code 页面可识别的代码 / UI-readable code
     * @param message 可展示提示 / display message
     */
    public record Response(String code, String message) {
    }
}

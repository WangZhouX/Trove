package io.trove.backend.advice;

import io.trove.backend.api.RedemptionDraftCreationException;
import io.trove.backend.controller.RedemptionDraftController;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * 为收件资料写入接口返回固定错误，避免响应包含原始字段。
 * Returns fixed errors for delivery detail writes to keep original field values out of responses.
 */
@RestControllerAdvice(assignableTypes = RedemptionDraftController.class)
public class RedemptionDraftErrorAdvice {

    /**
     * 将业务错误转成脱敏且不可缓存的响应。
     * Maps business failures to sanitized responses that cannot be cached.
     *
     * @param exception 草稿业务失败 / draft workflow failure
     * @return 对应 HTTP 状态和固定说明 / corresponding status and fixed feedback
     */
    @ExceptionHandler(RedemptionDraftCreationException.class)
    public ResponseEntity<RedemptionDraftCreationException.Response> businessError(
            RedemptionDraftCreationException exception) {
        return ResponseEntity.status(exception.status()).cacheControl(CacheControl.noStore())
                .body(exception.response());
    }

    /**
     * 忽略框架异常的输入原文，统一返回安全的格式提示。
     * Discards framework exceptions' input values and returns safe formatting feedback.
     *
     * @return 隐藏请求原文的 400 响应 / 400 response with no request values
     */
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<RedemptionDraftCreationException.Response> malformedRequest() {
        return businessError(new RedemptionDraftCreationException(RedemptionDraftCreationException.Error.INVALID_REQUEST));
    }
}

package io.trove.backend.advice;

import io.trove.backend.api.WalletAuthException;
import io.trove.backend.controller.WalletAuthController;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 将钱包登录接口的异常转换成固定的 HTTP 错误。
 * Converts wallet sign-in exceptions into stable HTTP errors.
 */
@RestControllerAdvice(assignableTypes = WalletAuthController.class)
public class WalletAuthErrorAdvice {

    /**
     * 把认证失败转换成前端可显示的固定错误。 / Maps authentication failures to stable client errors.
     *
     * @param exception 认证失败原因 / authentication failure reason
     * @return HTTP 错误状态和安全提示 / HTTP error status and safe feedback
     */
    @ExceptionHandler(WalletAuthException.class)
    public ResponseEntity<WalletAuthException.Response> authError(WalletAuthException exception) {
        var error = exception.error();
        return ResponseEntity.status(error.status()).body(error.response());
    }

    /**
     * 请求 JSON 或挑战编号无法解析时返回统一的 400 错误。
     * Produces a stable 400 error for malformed JSON or challenge identifiers.
     *
     * @return 固定格式的登录请求错误 / stable sign-in request error
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<WalletAuthException.Response> malformedRequest() {
        var error = WalletAuthException.Error.INVALID_REQUEST;
        return ResponseEntity.status(error.status()).body(error.response());
    }
}

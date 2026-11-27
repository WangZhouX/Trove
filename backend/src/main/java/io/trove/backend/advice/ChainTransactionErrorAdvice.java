package io.trove.backend.advice;

import io.trove.backend.api.ChainTransactionApi;
import io.trove.backend.api.ChainTransactionException;
import io.trove.backend.controller.ChainTransactionController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 将已索引链上交易回执的查询异常转换成固定的 HTTP 错误。
 * Converts indexed chain receipt lookup exceptions into stable HTTP errors.
 */
@RestControllerAdvice(assignableTypes = ChainTransactionController.class)
public class ChainTransactionErrorAdvice {

    /**
     * 将查询失败转换为可供页面处理的错误码。 / Maps lookup failures to a client-readable error code.
     *
     * @param failure 回执查询失败 / receipt lookup failure
     * @return HTTP 状态和安全的错误提示 / HTTP status and safe error feedback
     */
    @ExceptionHandler(ChainTransactionException.class)
    public ResponseEntity<ChainTransactionApi.ErrorResponse> lookupError(ChainTransactionException failure) {
        return ResponseEntity.status(failure.status()).body(failure.response());
    }
}

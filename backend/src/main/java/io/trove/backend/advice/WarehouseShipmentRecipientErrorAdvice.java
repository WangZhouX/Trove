package io.trove.backend.advice;

import io.trove.backend.api.WarehouseShipmentRecipientException;
import io.trove.backend.controller.WarehouseShipmentRecipientController;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * 返回脱敏且不可缓存的单笔收件资料查询错误。
 * Returns sanitized and non-cacheable errors for individual shipment recipient lookups.
 */
@RestControllerAdvice(assignableTypes = WarehouseShipmentRecipientController.class)
public class WarehouseShipmentRecipientErrorAdvice {

    /**
     * 将收件资料查询失败转换为不包含内部原因且不可缓存的响应。
     * Maps recipient lookup failures to non-cacheable responses without internal causes.
     *
     * @param exception 业务失败 / workflow failure
     * @return 错误状态和脱敏反馈 / error status and sanitized feedback
     */
    @ExceptionHandler(WarehouseShipmentRecipientException.class)
    public ResponseEntity<WarehouseShipmentRecipientException.Response> recipientError(
            WarehouseShipmentRecipientException exception) {
        return ResponseEntity.status(exception.status()).cacheControl(CacheControl.noStore()).body(exception.response());
    }

    /**
     * 任务编号解析失败时使用固定错误，避免回显请求参数。
     * Uses a fixed error when the shipment identifier cannot be parsed, without echoing the input.
     *
     * @return 不可缓存的 400 响应 / non-cacheable 400 response
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<WarehouseShipmentRecipientException.Response> malformedShipmentId() {
        return recipientError(new WarehouseShipmentRecipientException(WarehouseShipmentRecipientException.Error.INVALID_REQUEST));
    }
}

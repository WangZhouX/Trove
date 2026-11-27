package io.trove.backend.advice;

import io.trove.backend.api.FulfillmentException;
import io.trove.backend.controller.RedemptionController;
import io.trove.backend.controller.WarehouseShipmentController;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** 为兑换查询和仓库履约输出统一的业务错误。 / Supplies consistent errors for redemption queries and fulfillment. */
@RestControllerAdvice(assignableTypes = {RedemptionController.class, WarehouseShipmentController.class})
public class FulfillmentErrorAdvice {

    /**
     * 以指定状态码返回业务错误。 / Returns a business error with its specified HTTP status.
     *
     * @param exception 服务抛出的履约错误 / fulfillment failure raised by the service
     * @return 安全错误体和状态码 / safe error body and status
     */
    @ExceptionHandler(FulfillmentException.class)
    public ResponseEntity<FulfillmentException.Response> businessError(FulfillmentException exception) {
        return ResponseEntity.status(exception.status()).body(exception.response());
    }

    /**
     * 路径参数、筛选或 JSON 无法解析时，不回显原始参数。
     * Maps malformed path parameters, filters and JSON without echoing the raw input.
     *
     * @return 客户端可识别的 400 响应 / client-recognizable HTTP 400 response
     */
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<FulfillmentException.Response> malformedRequest() {
        return businessError(new FulfillmentException(FulfillmentException.Error.INVALID_FULFILLMENT_REQUEST));
    }
}

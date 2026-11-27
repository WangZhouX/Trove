package io.trove.backend.advice;

import io.trove.backend.api.ConsignmentException;
import io.trove.backend.controller.ConsignmentController;
import io.trove.backend.controller.WarehouseController;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * 只为寄存和仓库控制器输出稳定的寄存错误。
 * Provides stable consignment error responses for the consignment and warehouse controllers.
 */
@RestControllerAdvice(assignableTypes = {ConsignmentController.class, WarehouseController.class})
public class ConsignmentErrorAdvice {

    /**
     * 以业务状态码与安全提示响应失败。 / Map workflow failures to HTTP status and safe feedback.
     *
     * @param exception 服务层返回的业务失败 / workflow failure from the service
     * @return 稳定的业务错误响应 / stable workflow error response
     */
    @ExceptionHandler(ConsignmentException.class)
    public ResponseEntity<ConsignmentException.Response> businessError(ConsignmentException exception) {
        return ResponseEntity.status(exception.error().status()).body(exception.response());
    }

    /**
     * JSON、路径编号或分页参数无法解析时，返回统一的格式错误。
     * Maps malformed JSON, path IDs or pagination parameters to a stable format error.
     *
     * @return 客户端可识别的 400 响应 / client-recognizable 400 response
     */
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ConsignmentException.Response> malformedRequest() {
        var error = ConsignmentException.Error.INVALID_REQUEST;
        return ResponseEntity.status(error.status())
                .body(new ConsignmentException.Response(error.name(), error.message()));
    }
}

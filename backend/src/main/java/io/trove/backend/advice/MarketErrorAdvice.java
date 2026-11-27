package io.trove.backend.advice;

import io.trove.backend.api.MarketException;
import io.trove.backend.controller.CollectionController;
import io.trove.backend.controller.ItemController;
import io.trove.backend.controller.MarketController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * 将市场、藏品详情和收藏查询异常转换为统一的公开错误。
 * Maps market, item-detail and collection query failures to consistent public errors.
 */
@RestControllerAdvice(assignableTypes = {MarketController.class, ItemController.class, CollectionController.class})
public class MarketErrorAdvice {

    /**
     * 返回安全的业务错误提示。 / Return safe feedback for a business failure.
     *
     * @param exception 查询中产生的业务错误 / business error produced by the query
     * @return 对应状态码和错误体 / response with the mapped status and body
     */
    @ExceptionHandler(MarketException.class)
    public ResponseEntity<MarketException.Response> businessError(MarketException exception) {
        return ResponseEntity.status(exception.status()).body(exception.response());
    }

    /**
     * 分页、网络、品类或 UUID 参数无法绑定时返回 400。
     * Returns 400 when pagination, network, category or UUID parameters cannot be bound.
     *
     * @return 稳定的无效请求错误 / stable invalid-request response
     */
    @ExceptionHandler({MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<MarketException.Response> invalidParameters() {
        return businessError(new MarketException(MarketException.Error.MARKET_INVALID_REQUEST));
    }
}

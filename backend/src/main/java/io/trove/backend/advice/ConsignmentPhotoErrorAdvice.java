package io.trove.backend.advice;

import io.trove.backend.api.ConsignmentPhotoException;
import io.trove.backend.controller.ConsignmentPhotoController;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** 为照片入口输出安全且不可缓存的错误。 / Provides safe, uncacheable failures for private-photo routes. */
@RestControllerAdvice(assignableTypes = ConsignmentPhotoController.class)
public class ConsignmentPhotoErrorAdvice {

    /**
     * 使用稳定的错误代码与状态码。 / Uses the stable error code and status.
     *
     * @param error 受控照片失败 / controlled photo failure
     * @return 不含路径或文件内容的错误 / error without paths or file contents
     */
    @ExceptionHandler(ConsignmentPhotoException.class)
    public ResponseEntity<ConsignmentPhotoException.Response> photoFailure(ConsignmentPhotoException error) {
        return ResponseEntity.status(error.status()).cacheControl(CacheControl.noStore()).body(error.response());
    }

    /**
     * 将不合法的路径参数转为安全的 400 错误。 / Maps an invalid path parameter to a safe HTTP 400 error.
     *
     * @return 请求格式错误 / malformed request failure
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ConsignmentPhotoException.Response> invalidPath() {
        return photoFailure(new ConsignmentPhotoException(ConsignmentPhotoException.Error.INVALID_PHOTO_REQUEST));
    }
}

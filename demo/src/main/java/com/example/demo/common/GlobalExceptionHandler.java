package com.example.demo.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 将 Controller 和方法级安全异常统一转换为 {@link Result}，并保留正确 HTTP 状态。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** @param exception 参数校验异常。@return 400/10001 响应。 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleValidation(MethodArgumentNotValidException exception) {
        FieldError fieldError = exception.getBindingResult().getFieldError();
        String message = fieldError == null ? "请求参数不正确" : fieldError.getDefaultMessage();
        return ResponseEntity.badRequest().body(Result.failure(10001, message));
    }

    /** @param exception 请求绑定或参数类型异常。@return 400/10001 响应。 */
    @ExceptionHandler({ServletRequestBindingException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<Result<Void>> handleRequestBinding(Exception exception) {
        return ResponseEntity.badRequest().body(Result.failure(10001, "请求参数缺失或类型不正确"));
    }

    /** @param exception 业务规则主动抛出的异常。@return 根据业务码映射的失败响应。 */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusiness(BusinessException exception) {
        return ResponseEntity.status(toHttpStatus(exception.getCode()))
                .body(Result.failure(exception.getCode(), exception.getMessage()));
    }

    /** @param exception 数据库唯一索引冲突。@return 409/30001 响应。 */
    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<Result<Void>> handleDuplicateKey(DuplicateKeyException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Result.failure(30001, "数据已存在，请勿重复提交"));
    }

    /**
     * 处理 @PreAuthorize 等方法级鉴权失败。
     *
     * <p>请求级鉴权失败由 ApiAccessDeniedHandler 处理；方法级鉴权异常进入 MVC 后，
     * 需要在这里单独转换，避免被通用异常分支错误包装成 50000。</p>
     */
    /** @param exception 方法级鉴权拒绝异常。@return 403/10003 响应。 */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Result<Void>> handleAccessDenied(AccessDeniedException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Result.failure(10003, "没有执行该操作的权限"));
    }

    /** @param exception 未预料的系统异常。@return 500/50000 响应，同时记录完整错误日志。 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleUnexpected(Exception exception) {
        log.error("未处理的系统异常", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.failure(50000, "系统异常，请稍后重试"));
    }

    /** @param code FlowOps 业务错误码。@return 与业务语义对应的 HTTP 状态。 */
    private HttpStatus toHttpStatus(int code) {
        if (code == 10002) {
            return HttpStatus.UNAUTHORIZED;
        }
        if (code == 10003) {
            return HttpStatus.FORBIDDEN;
        }
        if (code == 10004) {
            return HttpStatus.TOO_MANY_REQUESTS;
        }
        if (code >= 50000) {
            return HttpStatus.SERVICE_UNAVAILABLE;
        }
        if (code >= 30000 && code < 40000) {
            return HttpStatus.CONFLICT;
        }
        if (code >= 20000 && code < 30000) {
            return HttpStatus.NOT_FOUND;
        }
        return HttpStatus.BAD_REQUEST;
    }
}

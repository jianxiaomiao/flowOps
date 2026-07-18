package com.example.demo.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** 将异常统一转换成 Result，并保留正确的 HTTP 状态。 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleValidation(MethodArgumentNotValidException exception) {
        FieldError fieldError = exception.getBindingResult().getFieldError();
        String message = fieldError == null ? "请求参数不正确" : fieldError.getDefaultMessage();
        return ResponseEntity.badRequest().body(Result.failure(10001, message));
    }

    @ExceptionHandler({ServletRequestBindingException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<Result<Void>> handleRequestBinding(Exception exception) {
        return ResponseEntity.badRequest().body(Result.failure(10001, "请求参数缺失或类型不正确"));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusiness(BusinessException exception) {
        return ResponseEntity.status(toHttpStatus(exception.getCode()))
                .body(Result.failure(exception.getCode(), exception.getMessage()));
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<Result<Void>> handleDuplicateKey(DuplicateKeyException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Result.failure(30001, "数据已存在，请勿重复提交"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleUnexpected(Exception exception) {
        log.error("未处理的系统异常", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.failure(50000, "系统异常，请稍后重试"));
    }

    private HttpStatus toHttpStatus(int code) {
        if (code >= 30000 && code < 40000) {
            return HttpStatus.CONFLICT;
        }
        if (code >= 20000 && code < 30000) {
            return HttpStatus.NOT_FOUND;
        }
        return HttpStatus.BAD_REQUEST;
    }
}

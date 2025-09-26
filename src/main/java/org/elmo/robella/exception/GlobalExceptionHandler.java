package org.elmo.robella.exception;

import lombok.extern.slf4j.Slf4j;

import org.elmo.robella.model.dto.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import cn.dev33.satoken.exception.NotLoginException;

/**
 * 全局异常处理器
 * 统一处理所有异常并返回标准化的错误响应
 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理Sa-Token未登录异常
     */
    @ExceptionHandler(NotLoginException.class)
    public ResponseEntity<ErrorResponse> handleNotLoginException(NotLoginException ex) {
        log.warn("Not login exception occurred: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse("UNAUTHORIZED", "User not authenticated");
        return ResponseEntity.status(401).body(error);
    }

    /**
     * 处理自定义业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException ex) {
        log.warn("Business exception occurred: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse(ex.getErrorCode(), ex.getMessage());
        return ResponseEntity.status(ex.getHttpStatus()).body(error);
    }

    /**
     * 处理API调用异常
     */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException ex) {
        log.error("API exception occurred: {}", ex.getMessage(), ex);
        ErrorResponse error = new ErrorResponse(ex.getErrorCode(), ex.getMessage());
        return ResponseEntity.status(ex.getHttpStatus()).body(error);
    }

    /**
     * 处理验证异常
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(ValidationException ex) {
        log.warn("Validation exception occurred: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse(ex.getErrorCode(), ex.getMessage());
        return ResponseEntity.status(ex.getHttpStatus()).body(error);
    }

    /**
     * 处理所有未捕获的异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("Unexpected exception occurred: {}", ex.getMessage(), ex);
        ErrorResponse error = new ErrorResponse("INTERNAL_ERROR", "An unexpected error occurred");
        return ResponseEntity.status(500).body(error);
    }
}
package org.elmo.robella.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * 全局异常处理器
 * 统一处理所有异常并返回标准化的错误响应
 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    
    /**
     * 处理业务异常
     * 所有继承自BaseBusinessException的异常都会被此方法处理
     */
    @ExceptionHandler(BaseBusinessException.class)
    public Mono<ResponseEntity<?>> handleBusinessException(
            BaseBusinessException ex, ServerWebExchange exchange) {
        
        String path = exchange.getRequest().getPath().value();
        
        // 根据异常级别记录日志
        if (shouldLogAsError(ex)) {
            log.error("Business exception [{}]: {}", ex.getErrorCode().getCode(), ex.getMessage(), ex);
        } else {
            log.warn("Business exception [{}]: {}", ex.getErrorCode().getCode(), ex.getMessage());
        }
        
        // 根据请求路径返回不同格式的响应
        return Mono.just(buildResponse(ex, path));
    }
    
    /**
     * 处理参数验证异常
     */
    @ExceptionHandler({WebExchangeBindException.class, BindException.class})
    public Mono<ResponseEntity<?>> handleValidationException(
            WebExchangeBindException ex, ServerWebExchange exchange) {
        
        log.warn("Validation failed: {}", ex.getMessage());
        
        Map<String, Object> details = new HashMap<>();
        for (FieldError fieldError : ex.getFieldErrors()) {
            details.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        
        InvalidParameterException validationEx = new InvalidParameterException(ErrorCode.VALIDATION_FAILED);
        validationEx.addDetails(details);
        
        return Mono.just(buildResponse(validationEx, exchange.getRequest().getPath().value()));
    }
    
    /**
     * 处理数据完整性违反异常（如唯一约束）
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public Mono<ResponseEntity<?>> handleDataIntegrityViolation(
            DataIntegrityViolationException ex, ServerWebExchange exchange) {
        
        log.warn("Data integrity violation: {}", ex.getMessage());
        
        DataConstraintException constraintEx = new DataConstraintException("data_constraint", ex.getMessage());
        return Mono.just(buildResponse(constraintEx, exchange.getRequest().getPath().value()));
    }
    
    /**
     * 处理参数类型异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public Mono<ResponseEntity<?>> handleIllegalArgument(
            IllegalArgumentException ex, ServerWebExchange exchange) {
        
        log.warn("Invalid argument: {}", ex.getMessage());
        
        InvalidParameterException invalidParamEx = new InvalidParameterException("argument", ex.getMessage());
        return Mono.just(buildResponse(invalidParamEx, exchange.getRequest().getPath().value()));
    }
    
    /**
     * 处理运行时异常
     */
    @ExceptionHandler(RuntimeException.class)
    public Mono<ResponseEntity<?>> handleRuntimeException(
            RuntimeException ex, ServerWebExchange exchange) {
        
        log.error("Runtime exception", ex);
        
        SystemException systemEx = new SystemException(ErrorCode.INTERNAL_ERROR) {};
        systemEx.addDetail("originalException", ex.getClass().getSimpleName());
        
        return Mono.just(buildResponse(systemEx, exchange.getRequest().getPath().value()));
    }
    
    /**
     * 处理所有其他异常
     */
    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<?>> handleGenericException(
            Exception ex, ServerWebExchange exchange) {
        
        log.error("Unhandled exception", ex);
        
        SystemException systemEx = new SystemException(ErrorCode.UNKNOWN_ERROR) {};
        systemEx.addDetail("originalException", ex.getClass().getSimpleName());
        
        return Mono.just(buildResponse(systemEx, exchange.getRequest().getPath().value()));
    }
    
    /**
     * 根据请求路径构建不同格式的响应
     */
    private ResponseEntity<?> buildResponse(BaseBusinessException ex, String path) {
        if (path.startsWith("/anthropic/")) {
            return ResponseEntity
                .status(ex.getHttpStatus())
                .body(AnthropicErrorResponse.fromException(ex));
        } else if (path.startsWith("/v1/")) {
            return ResponseEntity
                .status(ex.getHttpStatus())
                .body(OpenAIErrorResponse.fromException(ex));
        } else {
            return ResponseEntity
                .status(ex.getHttpStatus())
                .body(ErrorResponse.fromException(ex, path));
        }
    }
    
    /**
     * 判断是否应该作为错误级别记录日志
     * 系统错误和外部服务错误记录为ERROR级别
     * 其他业务异常记录为WARN级别
     */
    private boolean shouldLogAsError(BaseBusinessException ex) {
        return ex.getCategory() == ErrorCategory.SYSTEM || 
               ex.getCategory() == ErrorCategory.EXTERNAL_SERVICE;
    }
}
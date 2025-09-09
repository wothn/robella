package org.elmo.robella.exception;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    // 通用 API 错误结构
    @Data
    public static class ApiError {
        private String code;
        private String message;
        private String timestamp;
        private String traceId;
    }

    private Mono<ResponseEntity<ApiError>> buildApi(HttpStatus status, String code, String message) {
        ApiError error = new ApiError();
        error.setCode(code);
        error.setMessage(message);
        error.setTimestamp(Instant.now().toString());
        error.setTraceId(UUID.randomUUID().toString());
        return Mono.just(ResponseEntity.status(status).body(error));
    }

    // 兼容 Anthropic 风格输出
    private ResponseEntity<?> buildAnthropicStyle(int status, String type, String message) {
        var body = java.util.Map.of(
                "type", "error",
                "error", java.util.Map.of(
                        "type", type,
                        "message", message
                )
        );
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(body);
    }

    private ResponseEntity<?> buildGenericStyle(int status, String type, String message) {
        var body = java.util.Map.of(
                "error", java.util.Map.of(
                        "type", type,
                        "message", message
                )
        );
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(body);
    }

    private ResponseEntity<?> routeStyle(ServerWebExchange exchange, int status, String type, String message) {
        String path = exchange.getRequest().getPath().value();
        if (path.startsWith("/anthropic/")) {
            return buildAnthropicStyle(status, type, message);
        }
        return buildGenericStyle(status, type, message);
    }

  
    // ===== 业务相关异常处理 =====

    @ExceptionHandler(ProviderException.class)
    public Mono<ResponseEntity<?>> handleProvider(ProviderException ex, ServerWebExchange exchange) {
        log.error("Provider error", ex);
        return Mono.just(routeStyle(exchange, 502, "provider_error", ex.getMessage()));
    }

    @ExceptionHandler(TransformException.class)
    public Mono<ResponseEntity<?>> handleTransform(TransformException ex, ServerWebExchange exchange) {
        log.error("Transform error", ex);
        return Mono.just(routeStyle(exchange, 500, "transform_error", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Mono<ResponseEntity<ApiError>> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Invalid argument: {}", ex.getMessage());
        return buildApi(HttpStatus.BAD_REQUEST, "INVALID_ARGUMENT", ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ApiError>> handleGeneric(Exception ex) {
        log.error("Unhandled exception", ex);
        return buildApi(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "服务器内部错误");
    }
}
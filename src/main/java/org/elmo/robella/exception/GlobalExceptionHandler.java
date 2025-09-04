package org.elmo.robella.exception;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import reactor.core.publisher.Mono;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @Data
    public static class ErrorResponse {
        private String error;
        private String message;

        public ErrorResponse(String error, String message) {
            this.error = error;
            this.message = message;
        }
    }

    @ExceptionHandler(ProviderException.class)
    public Mono<ResponseEntity<?>> handleProviderException(ProviderException e, ServerWebExchange exchange) {
    log.error("Provider exception: ", e);
    String path = exchange.getRequest().getPath().value();
    if (path.startsWith("/anthropic/")) {
        var body = java.util.Map.of(
            "type", "error",
            "error", java.util.Map.of(
                "type", "provider_error",
                "message", e.getMessage()
            )
        );
        return Mono.just(ResponseEntity.status(502).contentType(MediaType.APPLICATION_JSON).body(body));
    }
    var body = java.util.Map.of(
        "error", java.util.Map.of(
            "type", "provider_error",
            "message", e.getMessage()
        )
    );
    return Mono.just(ResponseEntity.status(502).contentType(MediaType.APPLICATION_JSON).body(body));
    }

    @ExceptionHandler(TransformException.class)
    public Mono<ResponseEntity<?>> handleTransformException(TransformException e, ServerWebExchange exchange) {
        log.error("Transform exception: ", e);
        return Mono.just(buildGenericError(exchange, 500, "transform_error", e.getMessage()));
    }

    @ExceptionHandler(com.fasterxml.jackson.databind.JsonMappingException.class)
    public Mono<ResponseEntity<?>> handleJacksonException(com.fasterxml.jackson.databind.JsonMappingException e, ServerWebExchange exchange) {
        log.error("Jackson serialization error: ", e);
        return Mono.just(buildGenericError(exchange, 500, "serialization_error", "Failed to serialize response: " + e.getMessage()));
    }

    @ExceptionHandler(AuthenticationException.class)
    public Mono<ResponseEntity<?>> handleAuthenticationException(AuthenticationException e, ServerWebExchange exchange) {
        log.error("Authentication exception: {}", e.getMessage());
        return Mono.just(buildGenericError(exchange, 401, "authentication_error", e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Mono<ResponseEntity<?>> handleIllegalArgumentException(IllegalArgumentException e, ServerWebExchange exchange) {
        log.error("Illegal argument exception: ", e);
        return Mono.just(buildGenericError(exchange, 400, "illegal_argument", "Invalid argument: " + e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<?>> handleGeneralException(Exception e, ServerWebExchange exchange) {
    log.error("Unexpected exception: ", e);
    return Mono.just(buildGenericError(exchange, 500, "internal_error", "An unexpected error occurred"));
    }

    private ResponseEntity<?> buildGenericError(ServerWebExchange exchange, int status, String type, String message) {
    String path = exchange.getRequest().getPath().value();
    if (path.startsWith("/anthropic/")) {
        var body = java.util.Map.of(
            "type", "error",
            "error", java.util.Map.of(
                "type", type,
                "message", message
            )
        );
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(body);
    }
    var body = java.util.Map.of(
        "error", java.util.Map.of(
            "type", type,
            "message", message
        )
    );
    return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(body);
    }
}
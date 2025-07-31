package org.elmo.robella.exception;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
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
    public Mono<ResponseEntity<ErrorResponse>> handleProviderException(ProviderException e) {
        log.error("Provider exception: ", e);
        ErrorResponse error = new ErrorResponse("provider_error", e.getMessage());
        return Mono.just(ResponseEntity.status(502).body(error));
    }

    @ExceptionHandler(TransformException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleTransformException(TransformException e) {
        log.error("Transform exception: ", e);
        ErrorResponse error = new ErrorResponse("transform_error", e.getMessage());
        return Mono.just(ResponseEntity.status(500).body(error));
    }

    @ExceptionHandler(com.fasterxml.jackson.databind.JsonMappingException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleJacksonException(com.fasterxml.jackson.databind.JsonMappingException e) {
        log.error("Jackson serialization error: ", e);
        ErrorResponse error = new ErrorResponse("serialization_error", "Failed to serialize response: " + e.getMessage());
        return Mono.just(ResponseEntity.status(500).body(error));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleIllegalArgumentException(IllegalArgumentException e) {
        log.error("Illegal argument exception: ", e);
        ErrorResponse error = new ErrorResponse("illegal_argument", "Invalid argument: " + e.getMessage());
        return Mono.just(ResponseEntity.status(400).body(error));
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ErrorResponse>> handleGeneralException(Exception e) {
        log.error("Unexpected exception: ", e);
        ErrorResponse error = new ErrorResponse("internal_error", "An unexpected error occurred");
        return Mono.just(ResponseEntity.status(500).body(error));
    }
}
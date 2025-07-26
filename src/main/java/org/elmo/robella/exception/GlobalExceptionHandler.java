package org.elmo.robella.exception;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

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
    public ResponseEntity<ErrorResponse> handleProviderException(ProviderException e) {
        log.error("Provider exception: ", e);
        ErrorResponse error = new ErrorResponse("provider_error", e.getMessage());
        return ResponseEntity.status(502).body(error);
    }

    @ExceptionHandler(TransformException.class)
    public ResponseEntity<ErrorResponse> handleTransformException(TransformException e) {
        log.error("Transform exception: ", e);
        ErrorResponse error = new ErrorResponse("transform_error", e.getMessage());
        return ResponseEntity.status(500).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception e) {
        log.error("Unexpected exception: ", e);
        ErrorResponse error = new ErrorResponse("internal_error", "An unexpected error occurred");
        return ResponseEntity.status(500).body(error);
    }
}
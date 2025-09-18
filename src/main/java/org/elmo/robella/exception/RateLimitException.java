package org.elmo.robella.exception;

/**
 * 请求频率超限异常
 */
public class RateLimitException extends BusinessLogicException {
    
    public RateLimitException() {
        super(ErrorCode.RATE_LIMIT_EXCEEDED);
    }
    
    public RateLimitException(long retryAfterSeconds) {
        super(ErrorCode.RATE_LIMIT_EXCEEDED);
        addDetail("retryAfterSeconds", retryAfterSeconds);
    }
    
    public RateLimitException(String message, Throwable cause) {
        super(ErrorCode.RATE_LIMIT_EXCEEDED, cause);
        addDetail("details", message);
    }
    
    @Override
    public org.springframework.http.HttpStatus getHttpStatus() {
        return org.springframework.http.HttpStatus.TOO_MANY_REQUESTS;
    }
}
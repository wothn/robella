package org.elmo.robella.exception;

/**
 * 配额超限异常
 */
public class QuotaExceededException extends BusinessLogicException {
    
    public QuotaExceededException() {
        super(ErrorCode.QUOTA_EXCEEDED);
    }
    
    public QuotaExceededException(String quotaType, long current, long limit) {
        super(ErrorCode.QUOTA_EXCEEDED);
        addDetail("quotaType", quotaType);
        addDetail("current", current);
        addDetail("limit", limit);
    }
    
    public QuotaExceededException(String message, Throwable cause) {
        super(ErrorCode.QUOTA_EXCEEDED, cause);
        addDetail("details", message);
    }
    
    @Override
    public org.springframework.http.HttpStatus getHttpStatus() {
        return org.springframework.http.HttpStatus.TOO_MANY_REQUESTS;
    }
}
package org.elmo.robella.exception;

/**
 * 基础异常类
 * 所有自定义异常的父类
 */
public abstract class BaseException extends RuntimeException {
    
    private final String errorCode;
    private final int httpStatus;
    
    protected BaseException(String errorCode, String message, int httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }
    
    protected BaseException(String errorCode, String message, Throwable cause, int httpStatus) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public int getHttpStatus() {
        return httpStatus;
    }
}
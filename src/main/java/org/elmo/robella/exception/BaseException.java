package org.elmo.robella.exception;

import org.springframework.http.HttpStatus;

/**
 * 基础异常类
 * 所有自定义异常的父类
 */
public abstract class BaseException extends RuntimeException {
    
    private final String errorCode;
    private final HttpStatus httpStatus;
    
    protected BaseException(HttpStatus httpStatus, String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }
    
    protected BaseException(HttpStatus httpStatus, String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
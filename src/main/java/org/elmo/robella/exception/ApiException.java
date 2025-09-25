package org.elmo.robella.exception;

/**
 * API异常类
 * 用于处理API调用相关的异常
 */
public class ApiException extends BaseException {
    
    public ApiException(String errorCode, String message) {
        super(errorCode, message, 500);
    }
    
    public ApiException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, cause, 500);
    }
}
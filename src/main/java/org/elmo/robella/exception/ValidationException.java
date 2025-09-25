package org.elmo.robella.exception;

/**
 * 验证异常类
 * 用于处理参数验证相关的异常
 */
public class ValidationException extends BaseException {
    
    public ValidationException(String errorCode, String message) {
        super(errorCode, message, 400);
    }
    
    public ValidationException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, cause, 400);
    }
}
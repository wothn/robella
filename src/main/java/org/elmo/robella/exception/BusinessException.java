package org.elmo.robella.exception;

/**
 * 业务异常类
 * 用于处理业务逻辑相关的异常
 */
public class BusinessException extends BaseException {
    
    public BusinessException(String errorCode, String message) {
        super(errorCode, message, 400);
    }
    
    public BusinessException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, cause, 400);
    }
}
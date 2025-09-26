package org.elmo.robella.exception;

import org.springframework.http.HttpStatus;

/**
 * 验证异常类
 * 用于处理参数验证相关的异常
 */
public class ValidationException extends BaseException {
    
    /**
     * 构造函数
     * 默认状态码为400
     * @param errorCode 错误码
     * @param message   错误信息
     */
    public ValidationException(String errorCode, String message) {
        super(HttpStatus.BAD_REQUEST, errorCode, message);
    }
    
    /**
     * 构造函数
     * 默认状态码为400
     * @param errorCode 错误码
     * @param message   错误信息
     * @param cause     异常原因
     */
    public ValidationException(String errorCode, String message, Throwable cause) {
        super(HttpStatus.BAD_REQUEST, errorCode, message, cause);
    }
}
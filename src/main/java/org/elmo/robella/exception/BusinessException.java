package org.elmo.robella.exception;

import org.springframework.http.HttpStatus;

/**
 * 业务异常类
 * 用于处理业务逻辑相关的异常
 */
public class BusinessException extends BaseException {

    /**
     * 构造函数
     * 默认状态码为400
     * @param errorCode 错误码
     * @param message   错误信息
     */
    public BusinessException(String errorCode, String message) {
        super(HttpStatus.BAD_REQUEST, errorCode, message);
    }

    /**
     * 构造函数
     * 默认状态码为400
     * @param errorCode 错误码
     * @param message   错误信息
     * @param cause     异常原因
     */
    public BusinessException(String errorCode, String message, Throwable cause) {
        super(HttpStatus.BAD_REQUEST, errorCode, message, cause);
    }

    /**
     * 构造函数
     * @param httpStatus 状态码
     * @param errorCode  错误码
     * @param message    错误信息
     */
    public BusinessException(HttpStatus httpStatus, String errorCode, String message) {
        super(httpStatus, errorCode, message);
    }
    
    /**
     * 构造函数
     * @param httpStatus 状态码
     * @param errorCode  错误码
     * @param message    错误信息
     * @param cause      异常原因
     */
    public BusinessException(HttpStatus httpStatus, String errorCode, String message, Throwable cause) {
        super(httpStatus, errorCode, message, cause);
    }
}
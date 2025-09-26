package org.elmo.robella.exception;

import org.springframework.http.HttpStatus;

/**
 * API异常类
 * 用于处理API调用相关的异常
 */
public class ApiException extends BaseException {



    /**
     * 构造函数
     * 默认状态码为500
     * @param errorCode 错误码
     * @param message   错误信息
     */
    public ApiException(String errorCode, String message) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, errorCode, message);
    }

    /**
     * 构造函数
     * 默认状态码为500
     * @param errorCode 错误码
     * @param message   错误信息
     * @param cause     异常原因
     */
    public ApiException(String errorCode, String message, Throwable cause) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, errorCode, message, cause);
    }

    /**
     * 构造函数
     * @param httpStatus 状态码
     * @param errorCode  错误码
     * @param message    错误信息
     */
    public ApiException(HttpStatus httpStatus, String errorCode, String message) {
        super(httpStatus, errorCode, message);
    }

    /**
     * 构造函数
     * @param httpStatus 状态码
     * @param errorCode  错误码
     * @param message    错误信息
     * @param cause      异常原因
     */
    public ApiException(HttpStatus httpStatus, String errorCode, String message, Throwable cause) {
        super(httpStatus, errorCode, message, cause);
    }
}
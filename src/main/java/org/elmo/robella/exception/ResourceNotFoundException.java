package org.elmo.robella.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends BusinessException{

    /**
     * 
     * @param errorCode
     * @param message
     */
    public ResourceNotFoundException(String errorCode, String message) {
        super(HttpStatus.NOT_FOUND, errorCode, message);
    }
    /**
     * 构造函数
     * 默认状态码为404
     * @param errorCode 错误码
     * @param message   错误信息
     * @param cause     异常原因
     */
    public ResourceNotFoundException(String errorCode, String message, Throwable cause) {
        super(HttpStatus.NOT_FOUND, errorCode, message, cause);
    }
}

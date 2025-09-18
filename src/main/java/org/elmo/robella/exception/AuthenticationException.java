package org.elmo.robella.exception;

import org.springframework.http.HttpStatus;

/**
 * 认证异常基类
 * 用于处理身份认证相关的异常
 */
public abstract class AuthenticationException extends BaseBusinessException {
    
    protected AuthenticationException(ErrorCode errorCode, Object... messageArgs) {
        super(errorCode, messageArgs);
    }
    
    protected AuthenticationException(ErrorCode errorCode, Throwable cause, Object... messageArgs) {
        super(errorCode, cause, messageArgs);
    }
    
    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.UNAUTHORIZED;
    }
    
    @Override
    public ErrorCategory getCategory() {
        return ErrorCategory.AUTHENTICATION;
    }
}
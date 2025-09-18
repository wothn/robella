package org.elmo.robella.exception;

import org.springframework.http.HttpStatus;

/**
 * 授权异常基类
 * 用于处理权限和访问控制相关的异常
 */
public abstract class AuthorizationException extends BaseBusinessException {
    
    protected AuthorizationException(ErrorCode errorCode, Object... messageArgs) {
        super(errorCode, messageArgs);
    }
    
    protected AuthorizationException(ErrorCode errorCode, Throwable cause, Object... messageArgs) {
        super(errorCode, cause, messageArgs);
    }
    
    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.FORBIDDEN;
    }
    
    @Override
    public ErrorCategory getCategory() {
        return ErrorCategory.AUTHORIZATION;
    }
}
package org.elmo.robella.exception;

import org.springframework.http.HttpStatus;

/**
 * 外部服务异常基类
 * 用于处理第三方服务和外部系统相关的异常
 */
public abstract class ExternalServiceException extends BaseBusinessException {
    
    protected ExternalServiceException(ErrorCode errorCode, Object... messageArgs) {
        super(errorCode, messageArgs);
    }
    
    protected ExternalServiceException(ErrorCode errorCode, Throwable cause, Object... messageArgs) {
        super(errorCode, cause, messageArgs);
    }
    
    @Override
    public ErrorCategory getCategory() {
        return ErrorCategory.EXTERNAL_SERVICE;
    }
    
    /**
     * 默认返回502，表示网关错误
     */
    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.BAD_GATEWAY;
    }
}
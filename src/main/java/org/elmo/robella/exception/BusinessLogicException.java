package org.elmo.robella.exception;

import org.springframework.http.HttpStatus;

/**
 * 业务逻辑异常基类
 * 用于处理业务规则和逻辑相关的异常
 */
public abstract class BusinessLogicException extends BaseBusinessException {
    
    protected BusinessLogicException(ErrorCode errorCode, Object... messageArgs) {
        super(errorCode, messageArgs);
    }
    
    protected BusinessLogicException(ErrorCode errorCode, Throwable cause, Object... messageArgs) {
        super(errorCode, cause, messageArgs);
    }
    
    @Override
    public ErrorCategory getCategory() {
        return ErrorCategory.BUSINESS_LOGIC;
    }
    
    /**
     * 默认返回400，子类可以重写
     */
    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.BAD_REQUEST;
    }
}
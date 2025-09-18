package org.elmo.robella.exception;

import org.springframework.http.HttpStatus;

/**
 * 验证异常基类
 * 用于处理输入参数验证失败的情况
 */
public abstract class ValidationException extends BaseBusinessException {
    
    protected ValidationException(ErrorCode errorCode, Object... messageArgs) {
        super(errorCode, messageArgs);
    }
    
    protected ValidationException(ErrorCode errorCode, Throwable cause, Object... messageArgs) {
        super(errorCode, cause, messageArgs);
    }
    
    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.BAD_REQUEST;
    }
    
    @Override
    public ErrorCategory getCategory() {
        return ErrorCategory.VALIDATION;
    }
}
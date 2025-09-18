package org.elmo.robella.exception;

/**
 * 无效参数异常
 * 当请求参数格式错误或不符合要求时抛出
 */
public class InvalidParameterException extends ValidationException {
    
    public InvalidParameterException(String parameterName) {
        super(ErrorCode.INVALID_PARAMETER);
        addDetail("parameter", parameterName);
    }
    
    public InvalidParameterException(String parameterName, String reason) {
        super(ErrorCode.INVALID_PARAMETER);
        addDetail("parameter", parameterName);
        addDetail("reason", reason);
    }
    
    public InvalidParameterException(ErrorCode errorCode, Object... messageArgs) {
        super(errorCode, messageArgs);
    }
}
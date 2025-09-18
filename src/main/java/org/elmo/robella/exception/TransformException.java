package org.elmo.robella.exception;

/**
 * 数据转换异常
 * 当请求/响应转换失败时抛出
 */
public class TransformException extends ExternalServiceException {
    
    public TransformException(String message) {
        super(ErrorCode.TRANSFORM_ERROR, message);
    }
    
    public TransformException(String message, Throwable cause) {
        super(ErrorCode.TRANSFORM_ERROR, cause, message);
    }
    
    public TransformException(String from, String to, String message) {
        super(ErrorCode.TRANSFORM_ERROR, message);
        addDetail("fromFormat", from);
        addDetail("toFormat", to);
    }
    
    @Override
    public org.springframework.http.HttpStatus getHttpStatus() {
        return org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
    }
}
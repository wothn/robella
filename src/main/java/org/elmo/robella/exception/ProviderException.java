package org.elmo.robella.exception;

/**
 * 服务提供商异常
 * 当AI服务提供商返回错误时抛出
 */
public class ProviderException extends ExternalServiceException {
    
    public ProviderException(String message) {
        super(ErrorCode.PROVIDER_ERROR, message);
    }
    
    public ProviderException(String message, Throwable cause) {
        super(ErrorCode.PROVIDER_ERROR, cause, message);
    }
    
    public ProviderException(String provider, String errorCode, String message) {
        super(ErrorCode.PROVIDER_ERROR, message);
        addDetail("provider", provider);
        addDetail("providerErrorCode", errorCode);
    }
    
    public ProviderException(String provider, int httpStatus, String message) {
        super(ErrorCode.PROVIDER_ERROR, message);
        addDetail("provider", provider);
        addDetail("httpStatus", httpStatus);
    }
}
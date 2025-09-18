package org.elmo.robella.exception;

import org.springframework.http.HttpStatus;

/**
 * 系统异常基类
 * 用于处理系统内部错误
 */
public abstract class SystemException extends BaseBusinessException {
    
    protected SystemException(ErrorCode errorCode, Object... messageArgs) {
        super(errorCode, messageArgs);
    }
    
    protected SystemException(ErrorCode errorCode, Throwable cause, Object... messageArgs) {
        super(errorCode, cause, messageArgs);
    }
    
    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }
    
    @Override
    public ErrorCategory getCategory() {
        return ErrorCategory.SYSTEM;
    }
}

/**
 * 数据库异常
 * 当数据库操作失败时抛出
 */
class DatabaseException extends SystemException {
    
    public DatabaseException(String message) {
        super(ErrorCode.DATABASE_ERROR);
        addDetail("details", message);
    }
    
    public DatabaseException(String message, Throwable cause) {
        super(ErrorCode.DATABASE_ERROR, cause);
        addDetail("details", message);
    }
}

/**
 * 配置错误异常
 * 当系统配置错误时抛出
 */
class ConfigurationException extends SystemException {
    
    public ConfigurationException(String configKey) {
        super(ErrorCode.CONFIGURATION_ERROR);
        addDetail("configKey", configKey);
    }
    
    public ConfigurationException(String configKey, String message) {
        super(ErrorCode.CONFIGURATION_ERROR);
        addDetail("configKey", configKey);
        addDetail("details", message);
    }
}
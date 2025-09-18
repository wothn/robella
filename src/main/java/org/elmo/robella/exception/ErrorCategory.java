package org.elmo.robella.exception;

/**
 * 错误分类枚举
 * 用于对异常进行分类，便于监控和统计
 */
public enum ErrorCategory {
    
    /**
     * 验证错误 - 客户端输入验证失败
     */
    VALIDATION,
    
    /**
     * 认证错误 - 身份认证失败
     */
    AUTHENTICATION,
    
    /**
     * 授权错误 - 权限不足
     */
    AUTHORIZATION,
    
    /**
     * 业务逻辑错误 - 业务规则违反
     */
    BUSINESS_LOGIC,
    
    /**
     * 外部服务错误 - 第三方服务异常
     */
    EXTERNAL_SERVICE,
    
    /**
     * 系统错误 - 系统内部异常
     */
    SYSTEM
}
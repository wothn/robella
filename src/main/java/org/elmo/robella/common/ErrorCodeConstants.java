package org.elmo.robella.common;

/**
 * 错误码常量类
 * 定义项目中常用的错误码常量
 */
public final class ErrorCodeConstants {
    
    // 通用错误码 (10000-10999)
    public static final String INTERNAL_ERROR = "10000";
    public static final String SYSTEM_BUSY = "10001";
    public static final String SERVICE_UNAVAILABLE = "10002";
    public static final String TIMEOUT_ERROR = "10003";
    
    // 业务错误码 (20000-20999)
    public static final String INVALID_MODEL = "20001";
    public static final String RESOURCE_NOT_FOUND = "20002";
    public static final String RESOURCE_CONFLICT = "20003";
    public static final String ROUTING_CONFIG_ERROR = "20005";
    public static final String INSUFFICIENT_BALANCE = "20006";
    public static final String RATE_LIMIT_EXCEEDED = "20007";
    public static final String QUOTA_EXCEEDED = "20008";
    public static final String UNSUPPORTED_OPERATION = "20009";
    public static final String INVALID_API_KEY = "20010";
    public static final String USER_NOT_FOUND = "20011";
    
    // API调用错误码 (30000-30999)
    public static final String PROVIDER_ERROR = "30001";
    public static final String PROVIDER_AUTH_ERROR = "30002";
    public static final String PROVIDER_RATE_LIMIT = "30003";
    public static final String PROVIDER_TIMEOUT = "30004";
    public static final String PROVIDER_NETWORK_ERROR = "30005";
    public static final String PROVIDER_INVALID_RESPONSE = "30006";
    
    // 验证错误码 (40000-40999)
    public static final String INVALID_PARAMETER = "40001";
    public static final String MISSING_REQUIRED_FIELD = "40002";
    public static final String INVALID_FORMAT = "40003";
    public static final String INVALID_LENGTH = "40004";
    public static final String INVALID_RANGE = "40005";
    public static final String INVALID_EMAIL = "40006";
    public static final String INVALID_PHONE = "40007";
    public static final String INVALID_URL = "40008";
    public static final String INVALID_JSON = "40009";
    public static final String INVALID_TOKEN = "40010";
    
    // 认证授权错误码 (50000-50999)
    public static final String UNAUTHORIZED = "50001";
    public static final String FORBIDDEN = "50002";
    public static final String INVALID_CREDENTIALS = "50003";
    public static final String SESSION_EXPIRED = "50004";
    public static final String ACCOUNT_LOCKED = "50005";
    public static final String ACCOUNT_DISABLED = "50006";
    public static final String PERMISSION_DENIED = "50007";
    
    // 数据库错误码 (60000-60999)
    public static final String DATABASE_ERROR = "60001";
    public static final String RECORD_NOT_FOUND = "60002";
    public static final String RECORD_ALREADY_EXISTS = "60003";
    public static final String CONSTRAINT_VIOLATION = "60004";
    public static final String TRANSACTION_FAILED = "60005";
    
    private ErrorCodeConstants() {
        // 私有构造函数，防止实例化
    }
}
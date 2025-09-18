package org.elmo.robella.exception;

import lombok.Getter;

/**
 * 统一错误码枚举
 * 错误码格式：[类别前缀][4位数字]
 * V: 验证错误 (V1000-V1999)
 * A: 认证错误 (A2000-A2999)  
 * P: 权限错误 (P3000-P3999)
 * B: 业务错误 (B4000-B4999)
 * E: 外部服务错误 (E5000-E5999)
 * S: 系统错误 (S9000-S9999)
 */
@Getter
public enum ErrorCode {
    
    // ===== 验证错误 V1000-V1999 =====
    INVALID_PARAMETER("V1001", "参数无效"),
    MISSING_REQUIRED_PARAMETER("V1002", "缺少必需参数: %s"),
    DATA_CONSTRAINT_VIOLATION("V1003", "数据约束违反"),
    INVALID_REQUEST_FORMAT("V1004", "请求格式无效"),
    VALIDATION_FAILED("V1005", "数据验证失败"),
    
    // ===== 认证错误 A2000-A2999 =====
    INVALID_CREDENTIALS("A2001", "用户名或密码错误"),
    TOKEN_EXPIRED("A2002", "Token已过期"),
    TOKEN_INVALID("A2003", "Token无效"),
    REFRESH_TOKEN_EXPIRED("A2004", "刷新Token已过期"),
    REFRESH_TOKEN_INVALID("A2005", "刷新Token无效"),
    AUTHENTICATION_REQUIRED("A2006", "需要身份验证"),
    
    // ===== 权限错误 P3000-P3999 =====
    INSUFFICIENT_PERMISSION("P3001", "权限不足"),
    ACCESS_DENIED("P3002", "访问被拒绝"),
    ROLE_REQUIRED("P3003", "需要%s角色权限"),
    RESOURCE_ACCESS_DENIED("P3004", "资源访问被拒绝"),
    
    // ===== 业务错误 B4000-B4999 =====
    RESOURCE_NOT_FOUND("B4001", "%s不存在"),
    RESOURCE_CONFLICT("B4002", "%s已存在"),
    BUSINESS_RULE_VIOLATION("B4003", "业务规则违反: %s"),
    USER_DISABLED("B4004", "用户账户已禁用"),
    EMAIL_ALREADY_EXISTS("B4005", "邮箱已存在"),
    USERNAME_ALREADY_EXISTS("B4006", "用户名已存在"),
    QUOTA_EXCEEDED("B4007", "配额已超限"),
    RATE_LIMIT_EXCEEDED("B4008", "请求频率超限"),
    RESOURCE_IN_USE("B4009", "资源正在使用中"),
    INVALID_RESOURCE_STATE("B4010", "资源状态无效"),
    
    // ===== 外部服务错误 E5000-E5999 =====
    PROVIDER_ERROR("E5001", "服务提供商错误: %s"),
    TRANSFORM_ERROR("E5002", "数据转换错误: %s"),
    NETWORK_ERROR("E5003", "网络通信错误"),
    EXTERNAL_API_ERROR("E5004", "外部API错误"),
    TIMEOUT_ERROR("E5005", "请求超时"),
    
    // ===== 系统错误 S9000-S9999 =====
    INTERNAL_ERROR("S9001", "系统内部错误"),
    DATABASE_ERROR("S9002", "数据库错误"),
    CONFIGURATION_ERROR("S9003", "配置错误"),
    UNKNOWN_ERROR("S9999", "未知错误");
    
    private final String code;
    private final String messageTemplate;
    
    ErrorCode(String code, String messageTemplate) {
        this.code = code;
        this.messageTemplate = messageTemplate;
    }
    
    /**
     * 格式化消息
     * @param args 消息参数
     * @return 格式化后的消息
     */
    public String formatMessage(Object... args) {
        if (args.length == 0) {
            return messageTemplate;
        }
        try {
            return String.format(messageTemplate, args);
        } catch (Exception e) {
            return messageTemplate + " [参数格式化失败]";
        }
    }
}
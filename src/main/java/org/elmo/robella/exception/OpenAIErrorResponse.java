package org.elmo.robella.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

/**
 * OpenAI兼容错误响应格式
 * 用于/v1/*路径的API错误响应
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OpenAIErrorResponse {
    
    private OpenAIError error;
    
    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class OpenAIError {
        /**
         * 错误消息
         */
        private String message;
        
        /**
         * 错误类型
         */
        private String type;
        
        /**
         * 错误码（可选）
         */
        private String code;
        
        /**
         * 参数（可选）
         */
        private String param;
    }
    
    /**
     * 从业务异常创建OpenAI格式错误响应
     */
    public static OpenAIErrorResponse fromException(BaseBusinessException ex) {
        String errorType = mapToOpenAIErrorType(ex);
        String errorCode = mapToOpenAIErrorCode(ex.getErrorCode());
        
        return OpenAIErrorResponse.builder()
            .error(OpenAIError.builder()
                .message(ex.getFormattedMessage())
                .type(errorType)
                .code(errorCode)
                .build())
            .build();
    }
    
    /**
     * 创建简单错误响应
     */
    public static OpenAIErrorResponse create(String message, String type, String code) {
        return OpenAIErrorResponse.builder()
            .error(OpenAIError.builder()
                .message(message)
                .type(type)
                .code(code)
                .build())
            .build();
    }
    
    /**
     * 将业务异常映射为OpenAI错误类型
     */
    private static String mapToOpenAIErrorType(BaseBusinessException ex) {
        switch (ex.getCategory()) {
            case VALIDATION:
                return "invalid_request_error";
            case AUTHENTICATION:
                return "authentication_error";
            case AUTHORIZATION:
                return "permission_error";
            case BUSINESS_LOGIC:
                return "invalid_request_error";
            case EXTERNAL_SERVICE:
                return "api_error";
            case SYSTEM:
                return "server_error";
            default:
                return "server_error";
        }
    }
    
    /**
     * 将错误码映射为OpenAI错误码
     */
    private static String mapToOpenAIErrorCode(ErrorCode errorCode) {
        switch (errorCode) {
            case INVALID_PARAMETER:
            case MISSING_REQUIRED_PARAMETER:
                return "invalid_parameter";
            case INVALID_CREDENTIALS:
                return "invalid_authentication";
            case TOKEN_EXPIRED:
            case TOKEN_INVALID:
                return "invalid_authentication";
            case INSUFFICIENT_PERMISSION:
            case ACCESS_DENIED:
                return "insufficient_quota";
            case RATE_LIMIT_EXCEEDED:
                return "rate_limit_exceeded";
            case QUOTA_EXCEEDED:
                return "quota_exceeded";
            case PROVIDER_ERROR:
                return "api_error";
            default:
                return "server_error";
        }
    }
}
package org.elmo.robella.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

/**
 * Anthropic兼容错误响应格式
 * 用于/anthropic/*路径的API错误响应
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnthropicErrorResponse {
    
    /**
     * 固定值："error"
     */
    private String type = "error";
    
    private AnthropicError error;
    
    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class AnthropicError {
        /**
         * 错误类型
         */
        private String type;
        
        /**
         * 错误消息
         */
        private String message;
    }
    
    /**
     * 从业务异常创建Anthropic格式错误响应
     */
    public static AnthropicErrorResponse fromException(BaseBusinessException ex) {
        String errorType = mapToAnthropicErrorType(ex);
        
        return AnthropicErrorResponse.builder()
            .type("error")
            .error(AnthropicError.builder()
                .type(errorType)
                .message(ex.getFormattedMessage())
                .build())
            .build();
    }
    
    /**
     * 创建简单错误响应
     */
    public static AnthropicErrorResponse create(String type, String message) {
        return AnthropicErrorResponse.builder()
            .type("error")
            .error(AnthropicError.builder()
                .type(type)
                .message(message)
                .build())
            .build();
    }
    
    /**
     * 将业务异常映射为Anthropic错误类型
     */
    private static String mapToAnthropicErrorType(BaseBusinessException ex) {
        return switch (ex.getCategory()) {
            case VALIDATION -> "invalid_request_error";
            case AUTHENTICATION -> "authentication_error";
            case AUTHORIZATION -> "permission_error";
            case BUSINESS_LOGIC -> {
                if (ex.getErrorCode() == ErrorCode.RATE_LIMIT_EXCEEDED) {
                    yield "rate_limit_error";
                } else if (ex.getErrorCode() == ErrorCode.QUOTA_EXCEEDED) {
                    yield "overloaded_error";
                }
                yield "invalid_request_error";
            }
            case EXTERNAL_SERVICE -> "api_error";
            case SYSTEM -> "api_error";
            default -> "api_error";
        };
    }
}
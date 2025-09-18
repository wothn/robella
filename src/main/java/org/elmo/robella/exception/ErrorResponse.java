package org.elmo.robella.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

/**
 * 统一错误响应格式
 * 标准API使用此格式返回错误信息
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    
    private ErrorDetail error;
    
    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ErrorDetail {
        /**
         * 错误码
         */
        private String code;
        
        /**
         * 错误消息
         */
        private String message;
        
        /**
         * 错误分类
         */
        private String category;
        
        /**
         * 时间戳
         */
        private Instant timestamp;
        
        /**
         * 请求路径
         */
        private String path;
        
        /**
         * 详细信息
         */
        private Map<String, Object> details;
    }
    
    /**
     * 创建标准错误响应
     */
    public static ErrorResponse create(String code, String message, String category, 
                                     String path, Map<String, Object> details) {
        return ErrorResponse.builder()
            .error(ErrorDetail.builder()
                .code(code)
                .message(message)
                .category(category)
                .timestamp(Instant.now())
                .path(path)
                .details(details)
                .build())
            .build();
    }
    
    /**
     * 从业务异常创建错误响应
     */
    public static ErrorResponse fromException(BaseBusinessException ex, String path) {
        return create(
            ex.getErrorCode().getCode(),
            ex.getFormattedMessage(),
            ex.getCategory().name(),
            path,
            ex.getDetails().isEmpty() ? null : ex.getDetails()
        );
    }
}
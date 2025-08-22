package org.elmo.robella.model.anthropic.core;

import lombok.Data;

/**
 * Anthropic 错误响应
 */
@Data
public class AnthropicErrorResponse {
    
    /**
     * 错误信息
     */
    private AnthropicError error;
    
    /**
     * 错误详细信息
     */
    @Data
    public static class AnthropicError {
        
        /**
         * 错误类型
         */
        private String type;
        
        /**
         * 错误消息
         */
        private String message;
        
        /**
         * 错误代码
         */
        private String code;
    }
}

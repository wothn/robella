package org.elmo.robella.model.anthropic.stream;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Anthropic 错误事件
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AnthropicErrorEvent extends AnthropicStreamEvent {
    
    /**
     * 错误信息
     */
    private AnthropicError error;
    
    @Data
    public static class AnthropicError {
        /**
         * 错误类型 (如 "overloaded_error")
         */
        private String type;
        
        /**
         * 错误消息
         */
        private String message;
    }
}

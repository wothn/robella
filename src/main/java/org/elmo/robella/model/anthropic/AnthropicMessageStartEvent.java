package org.elmo.robella.model.anthropic;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Anthropic 消息开始事件
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AnthropicMessageStartEvent extends AnthropicStreamEvent {
    
    /**
     * 消息对象
     */
    private AnthropicMessage message;
}

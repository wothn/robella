package org.elmo.robella.model.anthropic.stream;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.elmo.robella.model.anthropic.core.AnthropicMessage;

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

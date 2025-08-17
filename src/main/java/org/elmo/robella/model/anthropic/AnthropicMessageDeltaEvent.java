package org.elmo.robella.model.anthropic;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Anthropic 消息增量事件
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AnthropicMessageDeltaEvent extends AnthropicStreamEvent {
    
    /**
     * 增量数据
     */
    private AnthropicDelta delta;
}

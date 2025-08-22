package org.elmo.robella.model.anthropic.stream;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Anthropic 内容块增量事件
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AnthropicContentBlockDeltaEvent extends AnthropicStreamEvent {
    
    /**
     * 内容块索引
     */
    private Integer index;
    
    /**
     * 增量数据
     */
    private AnthropicDelta delta;
}

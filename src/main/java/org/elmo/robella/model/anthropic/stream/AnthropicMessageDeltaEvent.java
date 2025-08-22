package org.elmo.robella.model.anthropic.stream;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.elmo.robella.model.anthropic.core.AnthropicUsage;

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

    /**
     * 使用量统计（递增）
     */
    private AnthropicUsage usage;
}

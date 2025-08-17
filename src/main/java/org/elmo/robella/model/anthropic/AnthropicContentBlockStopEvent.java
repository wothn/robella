package org.elmo.robella.model.anthropic;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Anthropic 内容块停止事件
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AnthropicContentBlockStopEvent extends AnthropicStreamEvent {
    
    /**
     * 内容块索引
     */
    private Integer index;
}

package org.elmo.robella.model.anthropic;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Anthropic 内容块开始事件
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AnthropicContentBlockStartEvent extends AnthropicStreamEvent {
    
    /**
     * 内容块索引
     */
    private Integer index;
    
    /**
     * 内容块对象
     */
    @JsonProperty("content_block")
    private AnthropicContent contentBlock;
}

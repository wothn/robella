package org.elmo.robella.model.anthropic.stream;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.elmo.robella.model.anthropic.content.AnthropicContent;

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

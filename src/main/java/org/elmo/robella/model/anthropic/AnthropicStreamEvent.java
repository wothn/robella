package org.elmo.robella.model.anthropic;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;

/**
 * Anthropic 流式事件基类
 */
@Data
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = AnthropicMessageStartEvent.class, name = "message_start"),
    @JsonSubTypes.Type(value = AnthropicContentBlockStartEvent.class, name = "content_block_start"),
    @JsonSubTypes.Type(value = AnthropicContentBlockDeltaEvent.class, name = "content_block_delta"),
    @JsonSubTypes.Type(value = AnthropicContentBlockStopEvent.class, name = "content_block_stop"),
    @JsonSubTypes.Type(value = AnthropicMessageDeltaEvent.class, name = "message_delta"),
    @JsonSubTypes.Type(value = AnthropicMessageStopEvent.class, name = "message_stop")
})
public abstract class AnthropicStreamEvent {
    
    /**
     * 事件类型
     */
    private String type;
}

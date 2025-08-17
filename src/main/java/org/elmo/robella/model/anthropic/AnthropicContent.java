package org.elmo.robella.model.anthropic;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;

/**
 * Anthropic 内容块基类
 */
@Data
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = AnthropicTextContent.class, name = "text"),
    @JsonSubTypes.Type(value = AnthropicImageContent.class, name = "image"),
    @JsonSubTypes.Type(value = AnthropicToolUseContent.class, name = "tool_use"),
    @JsonSubTypes.Type(value = AnthropicToolResultContent.class, name = "tool_result"),
    @JsonSubTypes.Type(value = AnthropicDocumentContent.class, name = "document")
})
public abstract class AnthropicContent {
    
    /**
     * 内容类型
     */
    private String type;
    
    /**
     * 缓存控制
     */
    @JsonProperty("cache_control")
    private AnthropicCacheControl cacheControl;
}

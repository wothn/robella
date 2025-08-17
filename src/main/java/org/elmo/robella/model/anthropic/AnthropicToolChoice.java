package org.elmo.robella.model.anthropic;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;

/**
 * Anthropic 工具选择基类
 */
@Data
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = AnthropicAutoToolChoice.class, name = "auto"),
    @JsonSubTypes.Type(value = AnthropicAnyToolChoice.class, name = "any"),
    @JsonSubTypes.Type(value = AnthropicSpecificToolChoice.class, name = "tool")
})
public abstract class AnthropicToolChoice {
    
    /**
     * 工具选择类型
     */
    private String type;
    
    /**
     * 是否禁用并行工具使用
     */
    @JsonProperty("disable_parallel_tool_use")
    private Boolean disableParallelToolUse = false;
}

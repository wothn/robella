package org.elmo.robella.model.anthropic;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;

/**
 * Anthropic 思考配置基类
 */
@Data
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = AnthropicEnabledThinking.class, name = "enabled"),
    @JsonSubTypes.Type(value = AnthropicDisabledThinking.class, name = "disabled")
})
public abstract class AnthropicThinking {
    
    /**
     * 思考模式类型
     */
    private String type;
}

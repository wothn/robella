package org.elmo.robella.model.anthropic.tool;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;

/**
 * Anthropic 工具定义基类
 */
@Data
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = AnthropicCustomTool.class, name = "custom"),
    @JsonSubTypes.Type(value = AnthropicComputerTool.class, name = "computer_20241022"),
    @JsonSubTypes.Type(value = AnthropicBashTool.class, name = "bash_20241022"),
    @JsonSubTypes.Type(value = AnthropicTextEditorTool.class, name = "text_editor_20241022")
})
public abstract class AnthropicTool {
    
    /**
     * 工具类型
     */
    private String type;
    
    /**
     * 工具名称
     */
    private String name;
    
}

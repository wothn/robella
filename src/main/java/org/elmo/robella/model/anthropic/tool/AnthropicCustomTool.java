package org.elmo.robella.model.anthropic.tool;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

/**
 * Anthropic 自定义工具
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AnthropicCustomTool extends AnthropicTool {
    
    /**
     * 工具描述
     */
    private String description;
    
    /**
     * 工具输入的 JSON Schema 定义
     */
    @JsonProperty("input_schema")
    private Object inputSchema;
}

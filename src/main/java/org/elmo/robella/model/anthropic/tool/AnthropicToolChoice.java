package org.elmo.robella.model.anthropic.tool;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Anthropic 工具选择统一类
 * 支持三种模式：auto、any、tool、none
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnthropicToolChoice {
    
    /**
     * 工具选择类型: "auto", "any", "tool"
     */
    private String type;
    
    /**
     * 指定要使用的工具名称 (仅当 type="tool" 时使用)
     */
    private String name;
    
    /**
     * 是否禁用并行工具使用
     */
    @JsonProperty("disable_parallel_tool_use")
    private Boolean disableParallelToolUse = false;

}

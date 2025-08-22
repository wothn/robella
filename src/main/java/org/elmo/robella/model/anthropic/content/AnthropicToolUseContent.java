package org.elmo.robella.model.anthropic.content;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

/**
 * Anthropic 工具使用内容块
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AnthropicToolUseContent extends AnthropicContent {
    
    /**
     * 工具使用的唯一标识符
     */
    private String id;
    
    /**
     * 工具名称
     */
    private String name;
    
    /**
     * 工具的输入参数对象
     */
    private Map<String, Object> input;
}

package org.elmo.robella.model.anthropic;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Anthropic 指定工具选择
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AnthropicSpecificToolChoice extends AnthropicToolChoice {
    
    /**
     * 指定要使用的工具名称
     */
    private String name;
}

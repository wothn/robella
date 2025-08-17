package org.elmo.robella.model.anthropic;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Anthropic 文本内容块
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AnthropicTextContent extends AnthropicContent {
    
    /**
     * 文本内容
     */
    private String text;
}

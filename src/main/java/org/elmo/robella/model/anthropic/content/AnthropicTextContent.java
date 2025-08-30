package org.elmo.robella.model.anthropic.content;

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
    
    /**
     * 获取内容类型
     */
    public String getType() {
        return "text";
    }
}

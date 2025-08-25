package org.elmo.robella.model.anthropic.content;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Anthropic 思考内容块
 * 用于表示模型的内部思考过程
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AnthropicThinkingContent extends AnthropicContent {
    
    /**
     * 思考内容
     */
    private String thinking;
}

package org.elmo.robella.model.openai.content;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * OpenAI 文本内容块
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OpenAITextContent extends OpenAIContent {
    
    /**
     * 文本内容
     */
    private String text;
}

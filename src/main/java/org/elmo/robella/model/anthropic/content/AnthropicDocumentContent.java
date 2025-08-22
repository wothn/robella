package org.elmo.robella.model.anthropic.content;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Anthropic 文档内容块
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AnthropicDocumentContent extends AnthropicContent {
    
    /**
     * 文档源数据
     */
    private AnthropicDocumentSource source;
}

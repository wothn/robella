package org.elmo.robella.model.anthropic;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

/**
 * Anthropic 文档内容块
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AnthropicDocumentContent extends AnthropicContent {
    
    /**
     * 文档源数据
     */
    private Map<String, Object> source;
}

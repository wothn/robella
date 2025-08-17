package org.elmo.robella.model.anthropic;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Anthropic 图片内容块
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AnthropicImageContent extends AnthropicContent {
    
    /**
     * 图片源数据
     */
    private AnthropicImageSource source;
}

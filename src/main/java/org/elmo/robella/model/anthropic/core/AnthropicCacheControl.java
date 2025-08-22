package org.elmo.robella.model.anthropic.core;

import lombok.Data;

/**
 * Anthropic 缓存控制
 */
@Data
public class AnthropicCacheControl {
    
    /**
     * 缓存类型，目前只支持 "ephemeral"
     */
    private String type;
}

package org.elmo.robella.model.anthropic.core;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Anthropic 使用量统计
 */
@Data
public class AnthropicUsage {
    
    /**
     * 使用的输入 token 数量
     */
    @JsonProperty("input_tokens")
    private Integer inputTokens;
    
    /**
     * 使用的输出 token 数量
     */
    @JsonProperty("output_tokens")
    private Integer outputTokens;
    
    /**
     * 创建缓存条目使用的输入 token 数量
     */
    @JsonProperty("cache_creation_input_tokens")
    private Integer cacheCreationInputTokens;
    
    /**
     * 从缓存读取的输入 token 数量
     */
    @JsonProperty("cache_read_input_tokens")
    private Integer cacheReadInputTokens;
}

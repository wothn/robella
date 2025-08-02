package org.elmo.robella.model.openai;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * OpenAI 使用统计信息
 */
@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Usage {
    
    /**
     * 完成生成的token数
     */
    @JsonProperty("completion_tokens")
    private Integer completionTokens;
    
    /**
     * 提示词token数
     */
    @JsonProperty("prompt_tokens")
    private Integer promptTokens;
    
    /**
     * 提示词中命中缓存的token数
     */
    @JsonProperty("prompt_cache_hit_tokens")
    private Integer promptCacheHitTokens;
    
    /**
     * 提示词中未命中缓存的token数
     */
    @JsonProperty("prompt_cache_miss_tokens")
    private Integer promptCacheMissTokens;
    
    /**
     * 总token数
     */
    @JsonProperty("total_tokens")
    private Integer totalTokens;
    
    /**
     * 完成token的详细信息
     */
    @JsonProperty("completion_tokens_details")
    private CompletionTokensDetails completionTokensDetails;
}

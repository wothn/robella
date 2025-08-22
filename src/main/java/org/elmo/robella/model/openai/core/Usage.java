package org.elmo.robella.model.openai.core;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * OpenAI 使用统计信息 (含扩展字段，向前兼容)
 */
@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Usage {

    /** 完成生成的 token 数 */
    @JsonProperty("completion_tokens")
    private Integer completionTokens;

    /** 提示词 token 数 */
    @JsonProperty("prompt_tokens")
    private Integer promptTokens;

    /** 提示词中命中缓存的 token 数 */
    @JsonProperty("prompt_cache_hit_tokens")
    private Integer promptCacheHitTokens;

    /** 提示词中未命中缓存的 token 数 */
    @JsonProperty("prompt_cache_miss_tokens")
    private Integer promptCacheMissTokens;

    /** 总 token 数 */
    @JsonProperty("total_tokens")
    private Integer totalTokens;

    /** 提示 token 详细信息 */
    @JsonProperty("prompt_tokens_details")
    private PromptTokensDetails promptTokensDetails;

    /** 完成 token 详细信息 */
    @JsonProperty("completion_tokens_details")
    private CompletionTokensDetails completionTokensDetails;
}

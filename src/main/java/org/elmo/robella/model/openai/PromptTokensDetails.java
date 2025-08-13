package org.elmo.robella.model.openai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * OpenAI 提示 token 详细信息 (兼容多厂商扩展字段)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class PromptTokensDetails {

    /** 音频转写产生的输入 token 数 */
    @JsonProperty("audio_tokens")
    private Integer audioTokens;

    /** 命中缓存的 token 数 */
    @JsonProperty("cached_tokens")
    private Integer cachedTokens;
}

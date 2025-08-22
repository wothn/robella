package org.elmo.robella.model.openai.core;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * OpenAI 完成 token 详细信息 (兼容思维链 / 预测接受拒绝 / 音频等扩展字段)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class CompletionTokensDetails {

    /** 推理模型产生的思维链 token 数 */
    @JsonProperty("reasoning_tokens")
    private Integer reasoningTokens;

    /** 被接受的预测 token 数 */
    @JsonProperty("accepted_prediction_tokens")
    private Integer acceptedPredictionTokens;

    /** 被拒绝的预测 token 数 */
    @JsonProperty("rejected_prediction_tokens")
    private Integer rejectedPredictionTokens;

    /** 生成的音频 token 数 */
    @JsonProperty("audio_tokens")
    private Integer audioTokens;
}

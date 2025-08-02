package org.elmo.robella.model.openai;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * OpenAI 完成token详细信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CompletionTokensDetails {
    
    /**
     * 推理模型产生的思维链token数量
     */
    @JsonProperty("reasoning_tokens")
    private Integer reasoningTokens;
}

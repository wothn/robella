package org.elmo.robella.model.anthropic;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Anthropic 启用思考模式
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AnthropicEnabledThinking extends AnthropicThinking {
    
    /**
     * 思考预算 token 数量
     */
    @JsonProperty("budget_tokens")
    private Integer budgetTokens;
}

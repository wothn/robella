package org.elmo.robella.model.anthropic.core;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Anthropic 思考配置
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnthropicThinking {
    
    /**
     * 思考模式类型: "enabled" 或 "disabled"
     */
    private String type;
    
    /**
     * 思考预算 token 数量（仅在 enabled 模式下使用）
     */
    @JsonProperty("budget_tokens")
    private Integer budgetTokens;

    /**
     * 创建启用思考模式的实例
     */
    public static AnthropicThinking enabled() {
        return new AnthropicThinking("enabled", null);
    }
    
    /**
     * 创建启用思考模式的实例，并指定预算 token 数量
     */
    public static AnthropicThinking enabled(Integer budgetTokens) {
        return new AnthropicThinking("enabled", budgetTokens);
    }
    
    /**
     * 创建禁用思考模式的实例
     */
    public static AnthropicThinking disabled() {
        return new AnthropicThinking("disabled", null);
    }
    
    /**
     * 判断是否启用思考模式
     */
    public boolean isEnabled() {
        return "enabled".equals(type);
    }
}

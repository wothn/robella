package org.elmo.robella.model.anthropic;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Anthropic 禁用思考模式
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AnthropicDisabledThinking extends AnthropicThinking {
    // 禁用模式没有额外的参数
}

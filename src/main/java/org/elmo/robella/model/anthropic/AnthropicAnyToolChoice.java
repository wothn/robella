package org.elmo.robella.model.anthropic;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Anthropic 任意工具选择
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AnthropicAnyToolChoice extends AnthropicToolChoice {
    // 任意模式没有额外的参数
}

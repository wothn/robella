package org.elmo.robella.model.anthropic;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Anthropic 自动工具选择
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AnthropicAutoToolChoice extends AnthropicToolChoice {
    // 自动模式没有额外的参数
}

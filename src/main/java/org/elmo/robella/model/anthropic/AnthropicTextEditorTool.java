package org.elmo.robella.model.anthropic;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Anthropic 文本编辑器工具
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AnthropicTextEditorTool extends AnthropicTool {
    // 文本编辑器工具没有额外的参数
}

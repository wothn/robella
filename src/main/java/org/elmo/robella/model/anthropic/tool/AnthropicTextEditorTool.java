package org.elmo.robella.model.anthropic.tool;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * Anthropic 文本编辑器工具
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AnthropicTextEditorTool extends AnthropicTool {
    // 文本编辑器工具没有额外的参数
}

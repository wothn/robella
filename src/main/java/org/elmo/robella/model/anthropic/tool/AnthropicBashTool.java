package org.elmo.robella.model.anthropic.tool;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Anthropic Bash 工具
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AnthropicBashTool extends AnthropicTool {
    // Bash 工具没有额外的参数
}

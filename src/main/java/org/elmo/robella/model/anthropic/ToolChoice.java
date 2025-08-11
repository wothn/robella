package org.elmo.robella.model.anthropic;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * tool_choice 参数结构。
 * mode:
 *  - auto (模型自动决定是否使用工具)
 *  - any  (强制使用任意一个工具)
 *  - tool (指定名称, 配合 name 字段)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ToolChoice {
    private String mode; // auto / any / tool
    private String name; // 当 mode=tool 时指定工具名
}

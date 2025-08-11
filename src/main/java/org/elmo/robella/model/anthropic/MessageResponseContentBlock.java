package org.elmo.robella.model.anthropic;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

/**
 * 响应中的 content block。
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MessageResponseContentBlock {
    private String type; // text, tool_use, tool_result, image, etc.
    private String text; // 当 type=text

    // tool_use
    private String id;   // tool_use id
    private String name; // tool 名称
    private Object input; // tool_use 输入（JSON结构）

    // tool_result
    private String toolUseId; // 对应的 tool_use id
    private String content;   // 结果文本
    private Boolean isError;  // 是否错误

    // image
    private ImageSource source; // 若为 image
}

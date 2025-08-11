package org.elmo.robella.model.anthropic;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Content Block: type 可以是 text / image / tool_result 等。
 * 当前先支持最常用的 text；其余类型预留字段结构。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnthropicContentBlock {

    private String type; // text, image, tool_use, tool_result, etc.

    // text
    private String text;

    // image (base64 或 URL) - 与 Anthropic 文档对应: {"type":"image","source":{...}}
    private ImageSource source;

    // tool_use: 模型请求调用某工具
    private String id; // tool_use id
    private String name; // tool 名称
    private Map<String, Object> input; // 输入参数

    // tool_result: 用户或系统返回的工具结果
    private String toolUseId; // 对应的 tool_use 里的 id (anthropic: tool_result.tool_use_id)
    private String content; // 工具结果的文本内容
    private Boolean isError; // 标记工具调用是否错误

    // 通用扩展
    private Map<String, Object> metadata;

    // 嵌套内容（未来可能支持）
    private List<AnthropicContentBlock> items;
}

package org.elmo.robella.model.anthropic;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 对应 Anthropic Messages API 的请求结构。
 * 参考官方文档：<a href="https://docs.anthropic.com/claude/reference/messages_post">...</a>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MessageRequest {

    /** 模型名称 (例如: claude-3-5-sonnet-20240620) */
    private String model;

    /** 会话消息列表（按照时间顺序）。*/
    private List<AnthropicMessage> messages;

    /** 系统提示词，可选。*/
    private String system;

    /**
     * 生成的最大输出 token 数 (必填)。
     * Anthropic 文档字段: max_tokens
     */
    @JsonProperty("max_tokens")
    private Integer maxTokens;

    /** 是否开启流式。*/
    private Boolean stream;

    /** 采样温度。*/
    private Double temperature;

    /** nucleus sampling (top_p)。*/
    @JsonProperty("top_p")
    private Double topP;

    /** top_k 采样。*/
    @JsonProperty("top_k")
    private Integer topK;

    /** 停止序列。*/
    @JsonProperty("stop_sequences")
    private List<String> stopSequences;

    /** 工具列表（可选）。*/
    private List<AnthropicTool> tools;

    /** 指定工具选择策略（auto / any / 指定某工具）。*/
    @JsonProperty("tool_choice")
    private ToolChoice toolChoice;

    /** 可附加的元数据。*/
    private Map<String, Object> metadata;
}

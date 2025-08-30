package org.elmo.robella.model.anthropic.stream;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.elmo.robella.model.anthropic.core.AnthropicUsage;

import java.util.Map;

/**
 * Anthropic 增量数据
 * 支持 TextDelta、InputJsonDelta、ThinkingDelta 等所有增量类型
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnthropicDelta {

    /**
     * 增量类型：text_delta, input_json_delta, thinking_delta
     */
    private String type;

    /**
     * 文本增量 (type = text_delta)
     */
    private String text;

    /**
     * 思考增量 (type = thinking_delta)
     */
    private String thinking;

    /**
     * 思考签名 (type = thinking_delta，可选)
     */
    private String signature;

    /**
     * 工具输入的增量JSON数据 (type = input_json_delta)
     */
    @JsonProperty("partial_json")
    private String partialJson;

    /**
     * 停止原因 (仅在 message_delta 事件中出现)
     * 可能值：end_turn, max_tokens, stop_sequence, tool_use
     */
    @JsonProperty("stop_reason")
    private String stopReason;

    /**
     * 停止序列 (仅在 message_delta 事件中出现)
     */
    @JsonProperty("stop_sequence")
    private String stopSequence;

    /**
     * 使用量统计 (仅在 message_delta 事件中出现)
     * 注意：只包含 output_tokens，input_tokens 在 message_start 中
     */
    private AnthropicUsage usage;

    // ==================== 辅助方法 ====================

    /**
     * 判断是否为文本增量
     */
    public boolean isTextDelta() {
        return "text_delta".equals(type);
    }

    /**
     * 判断是否为思考增量
     */
    public boolean isThinkingDelta() {
        return "thinking_delta".equals(type);
    }

    /**
     * 判断是否为工具输入增量
     */
    public boolean isInputJsonDelta() {
        return "input_json_delta".equals(type);
    }

    /**
     * 判断是否为签名增量
     */
    public boolean isSignatureDelta() {
        return "signature_delta".equals(type);
    }

    /**
     * 获取实际的增量内容（自动判断类型）
     */
    public String getDeltaContent() {
        if (isTextDelta()) {
            return text;
        } else if (isThinkingDelta()) {
            return thinking;
        } else if (isInputJsonDelta()) {
            return partialJson;
        }
        return null;
    }
}

package org.elmo.robella.model.anthropic.core;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.elmo.robella.model.anthropic.content.AnthropicContent;
import org.elmo.robella.model.anthropic.serializer.AnthropicMessageContentDeserializer;
import org.elmo.robella.model.anthropic.serializer.AnthropicMessageContentSerializer;

import java.util.ArrayList;
import java.util.List;

/**
 * Anthropic 消息模型
 * 包含完整的 Message 对象结构，既用于请求中的消息，也用于响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnthropicMessage {
    
    /**
     * 消息的唯一标识符（仅在响应中出现）
     */
    private String id;
    
    /**
     * 对象类型，始终为 "message"（仅在响应中出现）
     */
    private String type;
    
    /**
     * 消息角色：user 或 assistant
     */
    private String role;
    
    /**
     * 使用的模型名称（仅在响应中出现）
     */
    private String model;
    
    /**
     * 消息内容，可以是字符串或内容块数组
     * 使用自定义序列化器处理多态性
     */
    @JsonProperty("content")
    @JsonDeserialize(using = AnthropicMessageContentDeserializer.class)
    @JsonSerialize(using = AnthropicMessageContentSerializer.class)
    private List<AnthropicContent> content = new ArrayList<>();
    
    /**
     * 停止生成的原因（仅在响应中出现）
     * end_turn 正常结束 max_tokens 达到最大tokens限制 stop_sequence 遇到预设的停止序列 tool_use 需要调用工具
     */
    @JsonProperty("stop_reason")
    private String stopReason;
    
    /**
     * 生成的自定义停止序列（仅在响应中出现）
     */
    @JsonProperty("stop_sequence")
    private String stopSequence;
    
    /**
     * 使用量统计（仅在响应中出现）
     */
    private AnthropicUsage usage;

}

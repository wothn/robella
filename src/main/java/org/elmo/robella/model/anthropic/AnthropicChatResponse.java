package org.elmo.robella.model.anthropic;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * Anthropic Messages API 响应模型
 */
@Data
public class AnthropicChatResponse {
    
    /**
     * 响应的唯一标识符
     */
    private String id;
    
    /**
     * 对象类型，始终为 "message"
     */
    private String type;
    
    /**
     * 生成消息的会话角色，始终为 "assistant"
     */
    private String role;
    
    /**
     * 使用的模型名称
     */
    private String model;
    
    /**
     * 模型生成的内容
     */
    private List<AnthropicContent> content;
    
    /**
     * 停止生成的原因
     */
    @JsonProperty("stop_reason")
    private String stopReason;
    
    /**
     * 生成的自定义停止序列
     */
    @JsonProperty("stop_sequence")
    private String stopSequence;
    
    /**
     * 使用量统计
     */
    private AnthropicUsage usage;
}

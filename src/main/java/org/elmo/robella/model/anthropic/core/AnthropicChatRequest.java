package org.elmo.robella.model.anthropic.core;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.elmo.robella.model.anthropic.tool.AnthropicTool;
import org.elmo.robella.model.anthropic.tool.AnthropicToolChoice;

import java.util.List;

/**
 * Anthropic Messages API 请求模型
 */
@Data
public class AnthropicChatRequest {
    
    /**
     * 要使用的模型名称
     */
    private String model;
    
    /**
     * 生成的最大 token 数量
     */
    @JsonProperty("max_tokens")
    private Integer maxTokens;
    
    /**
     * 输入消息列表
     */
    private List<AnthropicMessage> messages;
    
    /**
     * 系统 prompt
     */
    private String system;
    
    /**
     * 控制生成随机性，0.0 - 1.0
     */
    private Double temperature;
    
    /**
     * 自定义的停止生成的文本序列
     */
    @JsonProperty("stop_sequences")
    private List<String> stopSequences;
    
    /**
     * 是否使用服务器发送事件 (SSE) 来增量返回响应内容
     */
    private Boolean stream;
    
    /**
     * 定义模型可能使用的工具
     */
    private List<AnthropicTool> tools;
    
    /**
     * 控制模型如何使用提供的工具
     */
    @JsonProperty("tool_choice")
    private AnthropicToolChoice toolChoice;
    
    /**
     * 使用 nucleus 采样
     */
    @JsonProperty("top_p")
    private Double topP;
    
    /**
     * 从 token 的前 K 个选项中采样
     */
    @JsonProperty("top_k")
    private Integer topK;
    
    /**
     * 请求元数据
     */
    private AnthropicMetadata metadata;
    
    /**
     * 配置 Claude 的扩展思考功能
     */
    private AnthropicThinking thinking;
}

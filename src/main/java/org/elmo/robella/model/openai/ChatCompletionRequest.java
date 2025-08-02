package org.elmo.robella.model.openai;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * OpenAI 聊天完成请求
 */
@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatCompletionRequest {
    
    /**
     * 使用的模型ID（必需）
     */
    private String model;
    
    /**
     * 对话消息列表（必需）
     */
    private List<ChatMessage> messages;
    
    /**
     * 频率惩罚，-2.0到2.0之间
     */
    @JsonProperty("frequency_penalty")
    private Double frequencyPenalty;
    
    /**
     * 最大token数，1到8192之间
     */
    @JsonProperty("max_tokens")
    private Integer maxTokens;
    
    /**
     * 存在惩罚，-2.0到2.0之间
     */
    @JsonProperty("presence_penalty")
    private Double presencePenalty;
    
    /**
     * 响应格式
     */
    @JsonProperty("response_format")
    private ResponseFormat responseFormat;
    
    /**
     * 停止序列
     */
    private Object stop; // String或String[]
    
    /**
     * 是否流式输出
     */
    private Boolean stream;
    
    /**
     * 流式输出选项
     */
    @JsonProperty("stream_options")
    private StreamOptions streamOptions;
    
    /**
     * 采样温度，0到2之间
     */
    private Double temperature;
    
    /**
     * Top-p采样，0到1之间
     */
    @JsonProperty("top_p")
    private Double topP;
    
    /**
     * 可用工具列表
     */
    private List<Tool> tools;
    
    /**
     * 工具选择控制
     */
    @JsonProperty("tool_choice")
    private Object toolChoice; // String或ToolChoice对象
    
    /**
     * 是否返回对数概率
     */
    private Boolean logprobs;
    
    /**
     * 返回top N token的对数概率
     */
    @JsonProperty("top_logprobs")
    private Integer topLogprobs;

    @JsonProperty("thinking")
    private Thinking thinking;

}

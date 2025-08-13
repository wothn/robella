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
     * 支持字符串形式："none", "auto", "required"
     * 或对象形式：{"type": "function", "function": {"name": "function_name"}}
     */
    @JsonProperty("tool_choice")
    private ToolChoice toolChoice;
    
    /**
     * 是否返回对数概率
     */
    private Boolean logprobs;
    
    /**
     * 返回top N token的对数概率
     * 整数，0到20之间，指定在每个token位置返回最可能的token数量，每个token都有相关的对数概率
     * 如果使用此参数，logprobs必须设置为true
     */
    @JsonProperty("top_logprobs")
    private Integer topLogprobs;

    @JsonProperty("thinking")
    private Thinking thinking;

    /**
     * 生成补全的token数上限，包括可见的token数和推理token数
     */
    @JsonProperty("max_completion_tokens")
    private Integer maxCompletionTokens;

    /**
     * 希望模型生成的输出类型
     */
    @JsonProperty("modalities")
    private List<String> modalities;

    /**
     * 模型生成的输出数量
     */
    @JsonProperty("n")
    private Integer n;

    /**
     * 是否并行调用工具调用
     */
    @JsonProperty("parallel_tool_calls")
    private Boolean parallelToolCalls;

    /**
     * 缓存的提示
     */
    @JsonProperty("prompt_cache_key")
    private String promptCacheKey;

    /**
     * 音频输出的参数
     */
    @JsonProperty("audio")
    private Audio audio;

    /**
     * 限制推理模型的推理工作量。目前支持的值为 minimal 、 low 、 medium 和 high 。减少推理工作量可以加快响应速度，并减少响应中用于推理的 token 数量。
     */
    @JsonProperty("reasoning_effort")
    private String reasoningEffort;

    /**
     * 文本输出的参数
     */
    @JsonProperty("text")
    private TextOptions text;

    /**
     * 额外的厂商特定参数
     */
    @JsonProperty("extra_body")
    private Object extraBody;
}

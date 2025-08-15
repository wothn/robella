package org.elmo.robella.model.openai;

import com.fasterxml.jackson.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenAI 聊天完成请求
 */
@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
@AllArgsConstructor
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
    @JsonAlias("max_completion_tokens")
    private Integer maxTokens;
    
    /**
     * 存在惩罚，-2.0到2.0之间
     */
    @JsonProperty("presence_penalty")
    private Double presencePenalty;

    /**
     * 预测参数
     */
    @JsonProperty("prediction")
    private Prediction prediction;
    /**
     * 响应格式
     */
    @JsonProperty("response_format")
    private ResponseFormat responseFormat;
    
    /**
     * 停止序列（统一使用数组表示；仍兼容单字符串输入）
     * 通过 @JsonFormat(ACCEPT_SINGLE_VALUE_AS_ARRAY) 允许客户端发送 "stop":"###" 这种单值形式
     */
    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    private List<String> stop;
    
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
     * OpenAI O系列推理努力程度。目前支持的值为 minimal 、 low 、 medium 和 high。
     */
    @JsonProperty("reasoning_effort")
    private String reasoningEffort;

    /**
     * 智谱系列思考参数
     */
    @JsonProperty("thinking")
    private Thinking thinking;

    /**
     * Qwen系列思考参数
     */
    @JsonProperty("enable_thinking")
    private Boolean enableThinking;

    /**
     * Qwen系列思考预算
     */
    @JsonProperty("thinking_budget")
    private Integer thinkingBudget;

    /**
     * 文本输出的参数
     */
    @JsonProperty("text")
    private TextOptions text;

    /**
     * 网络搜索选项
     * 此工具搜索网络以获取相关结果用于回复
     */
    @JsonProperty("web_search_options")
    private WebSearchOptions webSearchOptions;

    /**
     * 额外的厂商特定参数
     */
    @JsonProperty("extra_body")
    private Object extraBody;


    private Map<String, Object> undefined = new HashMap<>();

    @JsonAnySetter
    public void addUndefined(String key, Object value) {
        undefined.put(key, value);
    }

    @JsonAnyGetter
    public Map<String, Object> getUndefined() {
        return undefined;
    }

    /**
     * 网络搜索选项
     */
    @Data
    @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class WebSearchOptions {
        /**
         * 搜索上下文大小，默认值：medium，可选值为 low、medium 或 high
         */
        @JsonProperty("search_context_size")
        private String searchContextSize;

        /**
         * 搜索的近似位置参数
         */
        @JsonProperty("user_location")
        private UserLocation userLocation;
    }

    /**
     * 用户位置信息
     */
    @Data
    @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class UserLocation {
        /**
         * 搜索的近似位置参数
         */
        private Approximate approximate;
    }

    /**
     * 近似位置参数
     */
    @Data
    @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Approximate {
        /**
         * 用户城市的自由文本输入
         */
        private String city;

        /**
         * 用户的两字母 ISO 国家代码
         */
        private String country;

        /**
         * 用户地区的自由文本输入
         */
        private String region;

        /**
         * 用户的 IANA 时区
         */
        private String timezone;

        /**
         * 位置近似类型，始终为 approximate
         */
        private String type;
    }
}

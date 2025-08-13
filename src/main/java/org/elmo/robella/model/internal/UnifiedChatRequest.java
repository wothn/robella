package org.elmo.robella.model.internal;

import lombok.*;

import java.util.*;

/**
 * 扩展后的统一聊天请求：支持多模态、工具调用、结构化控制参数。
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class UnifiedChatRequest {

    private String model;                        // 逻辑模型名
    @Singular("message")
    private List<UnifiedChatMessage> messages;              // 对话历史
    private Boolean stream;                      // 是否流式
    private Integer maxTokens;                   // 最大生成 tokens
    private Double temperature;                  // 采样温度
    private Double topP;                         // top_p
    private Integer topK;                        // top_k（Anthropic等）
    private Double minP;                         // nucleus 下界 (部分厂商)
    private Double frequencyPenalty;             // 频率惩罚
    private Double presencePenalty;              // 存在惩罚
    private Long seed;                           // 随机种子
    private List<String> stop;                   // 停止序列
    @Singular("tool")
    private List<UnifiedTool> tools;             // 工具定义列表
    private Object toolChoice;                   // 工具选择策略：none/auto/required/{ name }
    private Boolean logprobs;                    // 是否需要对数概率
    private Integer topLogprobs;                 // top logprobs
    private Object responseFormat;               // 响应格式（预留，可为 JSON schema）
    private Object thinking;                     // 思维链/推理控制（预留对象）
    private Boolean parallelToolCalls;          // 是否并行调用工具（OpenAI 特有）
    private String systemMessage;               // 系统消息（Anthropic 专用字段）
    private Object cacheControl;                // 缓存控制（Anthropic 缓存机制）
    private List<String> modalities;            // 希望模型生成的输出类型（OpenAI multimodal）
    private Integer n;                           // 模型生成的输出数量（OpenAI）
    private String promptCacheKey;              // 缓存的提示（OpenAI prompt caching）
    private Object audio;                        // 音频输出参数（OpenAI audio output）
    private String reasoningEffort;             // 推理工作量限制（OpenAI o1 reasoning effort）
    private Object textOptions;                 // 文本输出参数（OpenAI text options）
    private Map<String, Object> vendorExtras;    // 厂商特定参数
    private Map<String, Object> metadata;        // 通用元数据（traceId / user / session 等）

}

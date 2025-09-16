package org.elmo.robella.model.internal;

import lombok.*;

import org.elmo.robella.common.ProviderType;
import org.elmo.robella.model.openai.audio.OpenAIAudio;
import org.elmo.robella.model.openai.core.*;
import org.elmo.robella.model.openai.tool.Tool;
import org.elmo.robella.model.openai.tool.ToolChoice;

import java.util.*;

/**
 * 扩展后的统一聊天请求：支持多模态、工具调用、结构化控制参数。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UnifiedChatRequest {

    private String model;                        // 逻辑模型名
    private ProviderType providerType;                 // 供应商名称，用于获取配置信息
    
    private List<OpenAIMessage> messages;          // 对话历史
    private Boolean stream;                      // 是否流式
    private StreamOptions streamOptions;         // 流式参数
    private Integer maxTokens;                   // 最大生成 tokens
    private Double temperature;                  // 采样温度
    private Double topP;                         // top_p
    private Integer topK;                        // top_k（Anthropic等）
    private Double minP;                         // nucleus 下界 (部分厂商)
    private Double frequencyPenalty;             // 频率惩罚
    private Double presencePenalty;              // 存在惩罚
    private Long seed;                           // 随机种子
    private List<String> stop;                   // 停止序列
    private List<Tool> tools;                    // 工具定义列表
    private ToolChoice toolChoice;               // 工具选择策略：none/auto/required/{ name }
    private Boolean logprobs;                    // 是否需要对数概率
    private Integer topLogprobs;                 // top logprobs
    private ResponseFormat responseFormat;       // 响应格式（预留，可为 JSON schema）
    private Boolean parallelToolCalls;           // 是否并行调用工具（OpenAI 特有）
    private Object cacheControl;                 // 缓存控制（Anthropic 缓存机制）
    private List<String> modalities;             // 希望模型生成的输出类型（OpenAI multimodal）
    private Integer n;                           // 模型生成的输出数量（OpenAI）
    private String promptCacheKey;               // 缓存的提示（OpenAI prompt caching）
    private OpenAIAudio audio;                         // 音频输出参数（OpenAI audio output）
    private TextOptions textOptions;             // 文本输出参数（OpenAI text options）
    private Prediction prediction;
    private ThinkingOptions thinkingOptions;
    private Map<String, Object> vendorExtras;    // 厂商特定参数

}



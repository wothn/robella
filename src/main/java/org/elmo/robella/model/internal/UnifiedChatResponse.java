package org.elmo.robella.model.internal;

import lombok.*;
import java.util.List;
import java.util.Map;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class UnifiedChatResponse {
    private String id;
    private String model;
    private String content;                  // 汇总纯文本（兼容）
    private UnifiedChatMessage assistantMessage; // 最终 assistant 消息多模态结构
    private List<UnifiedChatMessage> messages;    // 若厂商一次返回多条（例如 tool + assistant）
    private List<UnifiedToolCall> toolCalls; // 若存在工具调用
    private String reasoningContent;         // 推理内容汇总
    private String finishReason;             // stop / length / tool_calls ...
    private Usage usage;
    private UnifiedLogProbs logprobs;        // 对数概率信息（OpenAI logprobs）
    private List<UnifiedContentPart> audioOutputs; // 音频输出内容（OpenAI audio output）
    private List<String> warnings;           // 厂商警告信息
    private Map<String,Object> metadata;     // 通用元数据（对齐不同厂商）
    private Object rawVendor;                // 原始厂商响应（调试）

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Usage {
        private Integer promptTokens;
        private Integer completionTokens;
        private Integer totalTokens;
        
        // OpenAI o1 系列推理 tokens
        private Integer reasoningTokens;
        
        // Anthropic 缓存相关 tokens
        private Integer cachedTokens;
        private Integer cacheCreationInputTokens;
        private Integer cacheReadInputTokens;
        
        // 音频相关 tokens（OpenAI audio）
        private Integer audioTokens;
        
        // 其他厂商特定统计信息
        private Map<String, Object> extra;
    }
}

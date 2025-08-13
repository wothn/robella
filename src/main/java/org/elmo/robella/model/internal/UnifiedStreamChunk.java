package org.elmo.robella.model.internal;

import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UnifiedStreamChunk {
    private String messageId;                        // 当前增量所属消息 ID（若厂商提供）
    private String role;                             // 角色（assistant/tool）
    private String contentDelta;                    // 新增文本片段
    private String reasoningDelta;                  // 推理增量
    private List<UnifiedContentPart> contentParts;  // 新增的结构化内容片段（图像/音频段等）
    private List<UnifiedToolCallDelta> toolCallDeltas; // 工具调用增量
    private boolean finished;                       // 是否结束
    private String finishReason;                    // 结束原因（stop/length/tool_calls等）
    private UnifiedChatResponse.Usage usage;        // 流结束时可带 usage
    private UnifiedLogProbs logprobs;               // 流式对数概率信息
    private List<UnifiedContentPart> audioDeltas;   // 音频增量内容（OpenAI audio streaming）
    private Object rawVendor;                       // 原始事件（调试）

    public boolean hasPayload(){
        return (contentDelta != null && !contentDelta.isEmpty())
                || (reasoningDelta != null && !reasoningDelta.isEmpty())
                || (contentParts != null && !contentParts.isEmpty())
                || (toolCallDeltas != null && !toolCallDeltas.isEmpty())
                || (audioDeltas != null && !audioDeltas.isEmpty())
                || logprobs != null
                || usage != null
                || finished;
    }
}

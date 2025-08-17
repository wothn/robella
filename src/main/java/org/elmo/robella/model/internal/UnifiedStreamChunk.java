package org.elmo.robella.model.internal;

import lombok.*;
import org.elmo.robella.model.openai.Choice;
import org.elmo.robella.model.openai.Usage;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UnifiedStreamChunk {
    private String id;                        // 当前增量所属消息 ID（若厂商提供）
    private Long created;
    private String model;
    private List<Choice> choices;
    private String object;
    private String systemFingerprint;
    private Usage usage;

    // ---- 可选增量字段（不同厂商映射） ----
    private Boolean finished;                 // 是否结束事件
    private String contentDelta;              // 纯文本增量
    private String reasoningDelta;            // 思考/推理增量
    private List<Object> toolCallDeltas;      // 工具调用增量（泛型保留）

    public boolean isFinished() { return Boolean.TRUE.equals(finished); }
    public boolean hasPayload() {
        return isFinished() || (contentDelta!=null && !contentDelta.isEmpty()) ||
                (reasoningDelta!=null && !reasoningDelta.isEmpty()) ||
                (toolCallDeltas!=null && !toolCallDeltas.isEmpty()) || usage!=null;
    }


}

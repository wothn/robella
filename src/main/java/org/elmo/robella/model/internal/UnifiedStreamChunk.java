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


}

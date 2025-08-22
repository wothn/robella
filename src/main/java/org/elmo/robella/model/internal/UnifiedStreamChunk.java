package org.elmo.robella.model.internal;

import lombok.*;
import org.elmo.robella.model.openai.core.Choice;
import org.elmo.robella.model.openai.core.Usage;

import java.util.List;
import java.util.Map;

/**
 * 统一的流式增量数据结构
 * 支持OpenAI的chunk格式和Anthropic的event格式
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UnifiedStreamChunk {

    private String id;
    private Long created;
    private String model;
    private List<Choice> choices;
    private String object;
    private String systemFingerprint;
    private Usage usage;

}

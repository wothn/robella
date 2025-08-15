package org.elmo.robella.model.internal;

import lombok.*;
import org.elmo.robella.model.openai.*;

import java.util.List;
import java.util.Map;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class UnifiedChatResponse {
    private String id;
    private String model;
    private String object;
    private Long created;
    private List<Choice> choices;    // 若厂商一次返回多条（例如 tool + assistant）
    private Usage usage;
    private String systemFingerprint;
    private Map<String,Object> metadata;     // 通用元数据（对齐不同厂商）
    private Map<String,Object> undefined;

}

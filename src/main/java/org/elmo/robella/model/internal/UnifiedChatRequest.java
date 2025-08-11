package org.elmo.robella.model.internal;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class UnifiedChatRequest {
    private String model;                // 逻辑模型名（公共模型名）
    @Singular
    private List<Message> messages;      // 对话消息
    private Boolean stream;              // 是否流式
    private Integer maxTokens;           // 生成最大 tokens
    private Double temperature;          // 温度
    private Double topP;                 // top_p
    private Map<String, Object> vendorExtras; // 厂商特有参数

    @Data
    @Builder
    public static class Message {
        private String role;     // system / user / assistant / tool
        private String content;  // 纯文本（后续可扩展多模态）
    }
}

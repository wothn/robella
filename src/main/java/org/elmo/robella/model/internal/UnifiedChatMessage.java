package org.elmo.robella.model.internal;

import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 单条消息（多模态）
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class UnifiedChatMessage {
    private String id;                      // 消息 ID（若厂商提供）
    private String role;                     // system / user / assistant / tool
    @Singular("content")
    private List<UnifiedContentPart> contents; // 多模态内容
    private String name;                     // 参与者名称
    private String toolCallId;               // tool 消息对应的调用 id
    @Singular("toolCall")
    private List<UnifiedToolCall> toolCalls; // assistant 产生的工具调用列表
    private String reasoningContent;         // 推理可视化内容（若支持）
    @Singular("annotation")
    private List<UnifiedMessageAnnotation> annotations; // 消息注解（引用、工具调用等）
    private String refusal;                  // 拒绝回答的原因（OpenAI safety）
    private Object audio;                    // 音频内容（OpenAI audio output）
    private Map<String,Object> metadata;     // 通用元数据（如 source / lang / segmentIndex）
    private Map<String,Object> vendorExtras; // 厂商特定扩展（保持原始差异）
    private Object rawVendor;                // 原始厂商消息片段（调试/回放）

    /**
     * 兼容旧逻辑：返回纯文本拼接
     */
    public String aggregatedText() {
        if (contents == null) return null;
        StringBuilder sb = new StringBuilder();
        contents.forEach(c -> {
            if ("text".equals(c.getType()) && c.getText() != null) sb.append(c.getText());
        });
        return sb.toString();
    }

    public UnifiedChatMessage addText(String text) {
        if (contents == null) contents = new ArrayList<>();
        contents.add(UnifiedContentPart.text(text));
        return this;
    }
}
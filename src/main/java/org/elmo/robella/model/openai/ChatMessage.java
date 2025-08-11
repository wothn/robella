package org.elmo.robella.model.openai;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * OpenAI 聊天消息 (多模态强类型实现)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatMessage {
    
    /**
     * 消息角色：system, user, assistant, tool
     */
    private String role;
    
    /**
     * 多模态内容：统一使用 List<ContentPart>
     * 支持：纯字符串 / 数组（带 type 字段的对象）
     */
    @JsonProperty("content")
    @JsonDeserialize(using = ContentPartListDeserializer.class)
    @JsonSerialize(using = ContentPartListSerializer.class)
    @Builder.Default
    private List<ContentPart> content = new ArrayList<>();
    
    /**
     * 参与者名称（可选）
     */
    private String name;
    
    /**
     * 前缀模式（Beta功能，仅assistant消息）
     */
    private Boolean prefix;
    
    /**
     * 推理内容（Beta功能，仅assistant消息）
     */
    @JsonProperty("reasoning_content")
    private String reasoningContent;
    
    /**
     * Tool调用ID（仅tool消息）
     */
    @JsonProperty("tool_call_id")
    private String toolCallId;
    
    /**
     * Tool调用列表（仅assistant消息）
     */
    @JsonProperty("tool_calls")
    private List<ToolCall> toolCalls;

    // --- 工具方法 ---
    public static ChatMessage text(String role, String text) {
        return ChatMessage.builder().role(role).content(List.of(ContentPart.ofText(text))).build();
    }
    public ChatMessage addText(String text) {
        if (content == null) content = new ArrayList<>();
        content.add(ContentPart.ofText(text));
        return this;
    }
    public ChatMessage addImage(String url, String detail) {
        if (content == null) content = new ArrayList<>();
        content.add(ContentPart.ofImage(url, detail));
        return this;
    }
    public ChatMessage addAudio(String base64, String format) {
        if (content == null) content = new ArrayList<>();
        content.add(ContentPart.ofAudio(base64, format));
        return this;
    }
    public boolean isSingleText() {
        return content != null && content.size() == 1 && "text".equals(content.get(0).getType());
    }
    public String firstTextOrNull() {
        if (content == null) return null;
        return content.stream().filter(p -> Objects.equals(p.getType(), "text"))
                .map(ContentPart::getText).filter(Objects::nonNull).findFirst().orElse(null);
    }
}

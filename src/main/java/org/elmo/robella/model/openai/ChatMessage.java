package org.elmo.robella.model.openai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
@JsonIgnoreProperties(ignoreUnknown = true)
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
     * 推理内容（Beta功能，仅assistant消息）
     */
    @JsonProperty("reasoning_content")
    private String reasoningContent;
    
    /**
     * 拒绝消息（当模型拒绝回答时）
     */
    private String refusal;
    
    /**
     * 音频输出数据（当请求音频输出时）
     */
    private AudioData audio;
    
    /**
     * 消息注释列表（如URL引用等）
     */
    private List<MessageAnnotation> annotations;
    
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

}

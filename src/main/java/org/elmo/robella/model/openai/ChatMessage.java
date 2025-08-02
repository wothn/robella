package org.elmo.robella.model.openai;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.List;

/**
 * OpenAI 聊天消息
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
     * 消息内容
     */
    private String content;
    
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
}

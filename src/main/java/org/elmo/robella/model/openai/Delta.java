package org.elmo.robella.model.openai;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * OpenAI 流式响应增量
 */
@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Delta {
    
    /**
     * 角色（首次出现时）
     */
    private String role;
    
    /**
     * 内容增量
     */
    private String content;
    
    /**
     * 推理内容增量
     */
    @JsonProperty("reasoning_content")
    private String reasoningContent;
    
    /**
     * Tool调用增量
     */
    @JsonProperty("tool_calls")
    private List<ToolCall> toolCalls;
}

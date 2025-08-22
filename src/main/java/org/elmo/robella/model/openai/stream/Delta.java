package org.elmo.robella.model.openai.stream;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.elmo.robella.model.openai.content.OpenAIContent;
import org.elmo.robella.model.openai.tool.ToolCall;
import org.elmo.robella.model.openai.serializer.OpenAIContentListDeserializer;
import org.elmo.robella.model.openai.serializer.OpenAIContentListSerializer;

import java.util.List;

/**
 * OpenAI 流式响应增量 (多模态强类型)
 */
@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Delta {
    
    /**
     * 角色（首次出现时）
     */
    private String role;
    
    /**
     * 内容增量：可为空或包含一个或多个新增片段
     */
    @JsonDeserialize(using = OpenAIContentListDeserializer.class)
    @JsonSerialize(using = OpenAIContentListSerializer.class)
    private List<OpenAIContent> content;
    
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

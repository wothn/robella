package org.elmo.robella.model.openai;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * OpenAI 聊天完成选择
 */
@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Choice {
    
    /**
     * 完成索引
     */
    private Integer index;
    
    /**
     * 完成原因
     */
    @JsonProperty("finish_reason")
    private String finishReason;
    
    /**
     * 生成的消息
     */
    private ChatMessage message;
    
    /**
     * 对数概率信息
     */
    private LogProbs logprobs;
    
    /**
     * 流式增量（仅流式响应）
     */
    private Delta delta;
}

package org.elmo.robella.model.openai.core;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.elmo.robella.model.openai.stream.Delta;

/**
 * OpenAI 聊天完成选择
 */
@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Choice {

    private Integer index;            // 选项在选项列表中的索引。

    @JsonProperty("finish_reason")
    private String finishReason;     // 模型停止生成标记的原因。
    
    /**
     * 生成的消息
     */
    private OpenAIMessage message;
    
    /**
     * 对数概率信息
     */
    private LogProbs logprobs;
    
    /**
     * 流式增量（仅流式响应）
     */
    private Delta delta;
}

package org.elmo.robella.model.openai.core;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * OpenAI 对数概率信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LogProbs {
    
    /**
     * 消息内容token的对数概率信息列表
     */
    private List<TokenLogProb> content;
    
    /**
     * 消息拒绝token的对数概率信息列表
     */
    private List<TokenLogProb> refusal;
}

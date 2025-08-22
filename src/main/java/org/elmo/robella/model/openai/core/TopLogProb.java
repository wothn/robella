package org.elmo.robella.model.openai.core;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * OpenAI Top对数概率信息
 */
@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TopLogProb {
    
    /**
     * token
     */
    private String token;
    
    /**
     * 对数概率
     */
    private Double logprob;
    
    /**
     * token的UTF-8字节表示
     */
    private List<Integer> bytes;
    
    public TopLogProb(String token, Double logprob) {
        this.token = token;
        this.logprob = logprob;
    }
}

package org.elmo.robella.model.openai;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * OpenAI Token对数概率信息
 */
@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TokenLogProb {
    
    /**
     * 输出的token
     */
    private String token;
    
    /**
     * token的对数概率
     */
    private Double logprob;
    
    /**
     * token的UTF-8字节表示
     */
    private List<Integer> bytes;
    
    /**
     * top N token的对数概率列表
     */
    @JsonProperty("top_logprobs")
    private List<TopLogProb> topLogprobs;
    
    public TokenLogProb(String token, Double logprob) {
        this.token = token;
        this.logprob = logprob;
    }
}

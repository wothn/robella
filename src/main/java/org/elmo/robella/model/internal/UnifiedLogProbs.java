package org.elmo.robella.model.internal;

import lombok.*;
import java.util.List;

/**
 * 统一的对数概率信息结构
 * 覆盖 OpenAI logprobs 功能
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnifiedLogProbs {
    
    /**
     * 生成的 token 的对数概率信息列表
     */
    private List<TokenLogProb> content;
    
    /**
     * 单个 token 的对数概率信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TokenLogProb {
        /**
         * token 文本
         */
        private String token;
        
        /**
         * 该 token 的对数概率
         */
        private Double logprob;
        
        /**
         * UTF-8 字节表示（若适用）
         */
        private List<Integer> bytes;
        
        /**
         * top N 候选 token 的对数概率
         */
        private List<TopLogProb> topLogprobs;
    }
    
    /**
     * 候选 token 的对数概率信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopLogProb {
        /**
         * 候选 token 文本
         */
        private String token;
        
        /**
         * 该候选 token 的对数概率
         */
        private Double logprob;
        
        /**
         * UTF-8 字节表示（若适用）
         */
        private List<Integer> bytes;
    }
}

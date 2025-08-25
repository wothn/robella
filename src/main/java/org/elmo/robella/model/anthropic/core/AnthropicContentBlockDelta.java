package org.elmo.robella.model.anthropic.core;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Anthropic content_block_delta 事件数据
 */
@Data
public class AnthropicContentBlockDelta {
    
    @JsonProperty("type")
    private String type;
    
    @JsonProperty("index")
    private Integer index;
    
    @JsonProperty("delta")
    private Delta delta;
    
    @Data
    public static class Delta {
        @JsonProperty("type")
        private String type;
        
        @JsonProperty("text")
        private String text;
        
        @JsonProperty("partial_json")
        private String partialJson;
        
        @JsonProperty("thinking")
        private String thinking;
        
        @JsonProperty("signature")
        private String signature;
    }
}
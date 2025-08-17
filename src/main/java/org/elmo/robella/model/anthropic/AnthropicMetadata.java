package org.elmo.robella.model.anthropic;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Anthropic 请求元数据
 */
@Data
public class AnthropicMetadata {
    
    /**
     * 与请求关联的用户的外部标识符
     */
    @JsonProperty("user_id")
    private String userId;
}

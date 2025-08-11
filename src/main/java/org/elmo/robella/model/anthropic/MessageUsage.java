package org.elmo.robella.model.anthropic;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Tokens 使用情况。
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MessageUsage {
    @JsonProperty("input_tokens")
    private Integer inputTokens;

    @JsonProperty("output_tokens")
    private Integer outputTokens;
}

package org.elmo.robella.model.response;

import lombok.Data;

@Data
public class OpenAIUsage {
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;
}
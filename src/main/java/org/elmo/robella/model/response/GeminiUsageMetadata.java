package org.elmo.robella.model.response;

import lombok.Data;

@Data
public class GeminiUsageMetadata {
    private Integer promptTokenCount;
    private Integer candidatesTokenCount;
    private Integer totalTokenCount;
}
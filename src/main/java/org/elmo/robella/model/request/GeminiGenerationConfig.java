package org.elmo.robella.model.request;

import lombok.Data;

@Data
public class GeminiGenerationConfig {
    private Double temperature;
    private Integer maxOutputTokens;
}
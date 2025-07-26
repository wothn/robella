package org.elmo.robella.model.response;

import lombok.Data;

@Data
public class GeminiCandidate {
    private GeminiContent content;
    private String finishReason;
}
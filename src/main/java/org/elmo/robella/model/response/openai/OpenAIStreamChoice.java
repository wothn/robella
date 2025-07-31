package org.elmo.robella.model.response.openai;

import lombok.Data;

@Data
public class OpenAIStreamChoice {
    private Integer index;
    private OpenAIStreamDelta delta;
    private String finishReason;
}

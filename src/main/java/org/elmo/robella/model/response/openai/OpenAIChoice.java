package org.elmo.robella.model.response;

import lombok.Data;
import org.elmo.robella.model.common.OpenAIMessage;

@Data
public class OpenAIChoice {
    private Integer index;
    private OpenAIMessage message;
    private String finishReason;
}
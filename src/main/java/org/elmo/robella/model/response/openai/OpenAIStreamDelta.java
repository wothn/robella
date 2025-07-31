package org.elmo.robella.model.response.openai;

import lombok.Data;

@Data
public class OpenAIStreamDelta {
    private String role;
    private String content;
}

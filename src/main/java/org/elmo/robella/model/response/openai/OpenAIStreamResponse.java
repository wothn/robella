package org.elmo.robella.model.response.openai;

import lombok.Data;
import java.util.List;

@Data
public class OpenAIStreamResponse {
    private String id;
    private String object = "chat.completion.chunk";
    private Long created;
    private String model;
    private List<OpenAIStreamChoice> choices;
}

package org.elmo.robella.model.response;

import lombok.Data;
import java.util.List;

@Data
public class OpenAIChatResponse {
    private String id;
    private String object;
    private Long created;
    private String model;
    private List<OpenAIChoice> choices;
    private OpenAIUsage usage;
}
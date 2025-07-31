package org.elmo.robella.model.response.openai;

import lombok.Data;
import java.util.List;

@Data
public class OpenAIModelListResponse {
    private String object = "list";
    private List<OpenAIModel> data;
}
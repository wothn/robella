package org.elmo.robella.model.response;

import lombok.Data;
import java.util.List;

@Data
public class OpenAIModelListResponse {
    private String object = "list";
    private List<OpenAIModel> data;
}
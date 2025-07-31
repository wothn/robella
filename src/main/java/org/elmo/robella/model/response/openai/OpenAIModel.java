package org.elmo.robella.model.response.openai;

import lombok.Data;

@Data
public class OpenAIModel {
    private String id;
    private String object = "model";
    private Long created = System.currentTimeMillis() / 1000;
    private String ownedBy;
}
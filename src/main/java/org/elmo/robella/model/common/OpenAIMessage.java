package org.elmo.robella.model.common;

import lombok.Data;

@Data
public class OpenAIMessage {
    private String role;
    private String content;
}
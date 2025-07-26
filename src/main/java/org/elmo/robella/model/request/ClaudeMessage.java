package org.elmo.robella.model.request;

import lombok.Data;

@Data
public class ClaudeMessage {
    private String role;
    private String content;
}
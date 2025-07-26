package org.elmo.robella.model.request;

import lombok.Data;
import java.util.List;

@Data
public class ClaudeChatRequest {
    private String model;
    private List<ClaudeMessage> messages;
    private Double temperature;
    private Integer maxTokens;
    private Boolean stream = false;
}
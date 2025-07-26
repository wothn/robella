package org.elmo.robella.model.response;

import lombok.Data;
import java.util.List;

@Data
public class ClaudeChatResponse {
    private String id;
    private String type;
    private String role;
    private List<ClaudeContent> content;
    private String model;
    private ClaudeUsage usage;
}
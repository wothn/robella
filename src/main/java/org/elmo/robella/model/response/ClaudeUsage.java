package org.elmo.robella.model.response;

import lombok.Data;

@Data
public class ClaudeUsage {
    private Integer inputTokens;
    private Integer outputTokens;
}
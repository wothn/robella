package org.elmo.robella.model.internal;

import lombok.*;

/**
 * Thinking options for AI models that support reasoning features.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ThinkingOptions {
    private String type;
    private String reasoningEffort;
    private Integer thinkingBudget;
}
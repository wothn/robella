package org.elmo.robella.model.response;

import lombok.Data;
import java.util.List;

@Data
public class GeminiChatResponse {
    private List<GeminiCandidate> candidates;
    private GeminiUsageMetadata usageMetadata;
}
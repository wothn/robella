package org.elmo.robella.model.request;

import lombok.Data;
import java.util.List;

@Data
public class GeminiChatRequest {
    private List<GeminiContent> contents;
    private GeminiGenerationConfig generationConfig;
}
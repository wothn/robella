package org.elmo.robella.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenUsageResponse {
    private Long totalPromptTokens;
    private Long totalCompletionTokens;
    private Long totalTokens;
    private Double averagePromptTokensPerRequest;
    private Double averageCompletionTokensPerRequest;
    private Double averageTokensPerRequest;
    private OffsetDateTime periodStart;
    private OffsetDateTime periodEnd;
}
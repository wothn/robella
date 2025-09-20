package org.elmo.robella.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelPopularityResponse {
    private List<ModelStats> models;
    private OffsetDateTime periodStart;
    private OffsetDateTime periodEnd;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ModelStats {
        private String modelKey;
        private Long requestCount;
        private Long totalTokens;
        private BigDecimal totalCost;
        private Double successRate;
        private Double averageDurationMs;
    }
}
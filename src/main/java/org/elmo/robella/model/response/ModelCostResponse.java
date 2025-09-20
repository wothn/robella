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
public class ModelCostResponse {
    private List<ModelCostStats> models;
    private OffsetDateTime periodStart;
    private OffsetDateTime periodEnd;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ModelCostStats {
        private String modelKey;
        private BigDecimal totalCost;
        private Long totalTokens;
        private BigDecimal averageCostPerToken;
        private BigDecimal averageCostPerRequest;
        private Long requestCount;
    }
}
package org.elmo.robella.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserOverviewResponse {
    private Long userId;
    private Long totalRequests;
    private Long successfulRequests;
    private Long failedRequests;
    private Long totalTokens;
    private BigDecimal totalCost;
    private Double averageDurationMs;
    private Double averageTokensPerSecond;
    private Double errorRate;
    private Integer uniqueModelsUsed;
    private OffsetDateTime periodStart;
    private OffsetDateTime periodEnd;
}
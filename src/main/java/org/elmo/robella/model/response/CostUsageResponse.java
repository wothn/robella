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
public class CostUsageResponse {
    private BigDecimal totalInputCost;
    private BigDecimal totalOutputCost;
    private BigDecimal totalCost;
    private Double averageInputCostPerRequest;
    private Double averageOutputCostPerRequest;
    private Double averageCostPerRequest;
    private BigDecimal averageCostPerToken;
    private String currency;
    private OffsetDateTime periodStart;
    private OffsetDateTime periodEnd;
}
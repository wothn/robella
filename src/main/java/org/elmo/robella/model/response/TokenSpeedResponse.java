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
public class TokenSpeedResponse {
    private BigDecimal averageTokensPerSecond;
    private BigDecimal maxTokensPerSecond;
    private BigDecimal minTokensPerSecond;
    private BigDecimal medianTokensPerSecond;
    private OffsetDateTime periodStart;
    private OffsetDateTime periodEnd;
}
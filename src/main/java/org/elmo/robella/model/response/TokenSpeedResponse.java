package org.elmo.robella.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenSpeedResponse {
    private BigDecimal averageTokensPerSecond;
    private BigDecimal maxTokensPerSecond;
    private BigDecimal minTokensPerSecond;
    private BigDecimal medianTokensPerSecond;
    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;
}
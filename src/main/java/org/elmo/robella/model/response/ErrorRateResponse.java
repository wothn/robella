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
public class ErrorRateResponse {
    private Double overallErrorRate;
    private Long totalRequests;
    private Long failedRequests;
    private OffsetDateTime periodStart;
    private OffsetDateTime periodEnd;
}
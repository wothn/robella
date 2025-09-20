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
public class RequestUsageResponse {
    private Long totalRequests;
    private Long successfulRequests;
    private Long failedRequests;
    private Long streamRequests;
    private Long nonStreamRequests;
    private Double successRate;
    private Double errorRate;
    private Double streamRate;
    private OffsetDateTime periodStart;
    private OffsetDateTime periodEnd;
}
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
public class LatencyStatsResponse {
    private Double averageDurationMs;
    private Double minDurationMs;
    private Double maxDurationMs;
    private Double medianDurationMs;
    private Double p95DurationMs;
    private Double p99DurationMs;
    private Double averageFirstTokenLatencyMs;
    private OffsetDateTime periodStart;
    private OffsetDateTime periodEnd;
}
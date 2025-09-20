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
public class TimeSeriesResponse {
    private String interval;
    private List<TimeSeriesDataPoint> dataPoints;
    private OffsetDateTime periodStart;
    private OffsetDateTime periodEnd;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimeSeriesDataPoint {
        private OffsetDateTime timestamp;
        private Long requestCount;
        private Long totalTokens;
        private BigDecimal totalCost;
        private Long successfulRequests;
        private Long failedRequests;
    }
}
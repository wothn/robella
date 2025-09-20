package org.elmo.robella.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorByModelResponse {
    private List<ModelErrorStats> models;
    private OffsetDateTime periodStart;
    private OffsetDateTime periodEnd;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ModelErrorStats {
        private String modelKey;
        private Long totalRequests;
        private Long failedRequests;
        private Double errorRate;
    }
}
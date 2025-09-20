package org.elmo.robella.model.entity;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Table("request_log")
public class RequestLog {

    @Id
    private Long id;

    private String requestId;

    @Column("user_id")
    private Long userId;

    @Column("api_key_id")
    private Long apiKeyId;

    @Column("model_key")
    private String modelKey;

    @Column("vendor_model_key")
    private String vendorModelKey;

    @Column("provider_id")
    private Long providerId;

    @Column("endpoint_type")
    private String endpointType;

    @Column("prompt_tokens")
    private Integer promptTokens;

    @Column("completion_tokens")
    private Integer completionTokens;

    @Column("total_tokens")
    private Integer totalTokens;

    @Column("token_source")
    private String tokenSource;

    @Column("input_cost")
    private BigDecimal inputCost;

    @Column("output_cost")
    private BigDecimal outputCost;

    @Column("total_cost")
    private BigDecimal totalCost;

    @Column("currency")
    private String currency;

    @Column("duration_ms")
    private Integer durationMs;

    @Column("first_token_latency_ms")
    private Integer firstTokenLatencyMs;

    @Column("tokens_per_second")
    private BigDecimal tokensPerSecond;

    @Column("is_stream")
    private Boolean isStream;

    @Column("is_success")
    private Boolean isSuccess;
    @Column("created_at")
    private OffsetDateTime createdAt;
}
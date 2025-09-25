package org.elmo.robella.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
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
@TableName("request_log")
public class RequestLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String requestId;

    @TableField("user_id")
    private Long userId;

    @TableField("api_key_id")
    private Long apiKeyId;

    @TableField("model_key")
    private String modelKey;

    @TableField("vendor_model_key")
    private String vendorModelKey;

    @TableField("provider_id")
    private Long providerId;

    @TableField("endpoint_type")
    private String endpointType;

    @TableField("prompt_tokens")
    private Integer promptTokens;

    @TableField("cached_tokens")
    private Integer cachedTokens;

    @TableField("completion_tokens")
    private Integer completionTokens;

    @TableField("total_tokens")
    private Integer totalTokens;

    @TableField("token_source")
    private String tokenSource;

    @TableField("input_cost")
    private BigDecimal inputCost;

    @TableField("output_cost")
    private BigDecimal outputCost;

    @TableField("total_cost")
    private BigDecimal totalCost;

    @TableField("currency")
    private String currency;

    @TableField("duration_ms")
    private Integer durationMs;

    @TableField("first_token_latency_ms")
    private Integer firstTokenLatencyMs;

    @TableField("tokens_per_second")
    private BigDecimal tokensPerSecond;

    @TableField("is_stream")
    private Boolean isStream;

    @TableField("is_success")
    private Boolean isSuccess;

    @TableField("created_at")
    private OffsetDateTime createdAt;
}
package org.elmo.robella.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import lombok.Data;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@TableName("pricing_tier")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PricingTier {
    @TableId(type = IdType.AUTO)
    private Long id;

    @NotNull(message = "Vendor model ID cannot be null")
    @TableField("vendor_model_id")
    private Long vendorModelId;

    @NotNull(message = "Tier number cannot be null")
    @TableField("tier_number")
    private Integer tierNumber;

    @NotNull(message = "Min tokens cannot be null")
    @TableField("min_tokens")
    private Long minTokens;

    @TableField("max_tokens")
    private Long maxTokens;

    @NotNull(message = "Input price per million tokens cannot be null")
    @DecimalMin(value = "0.0", message = "Input price must be non-negative")
    @JsonSerialize(using = ToStringSerializer.class)
    @TableField("input_per_million_tokens")
    private BigDecimal inputPerMillionTokens;

    @NotNull(message = "Output price per million tokens cannot be null")
    @DecimalMin(value = "0.0", message = "Output price must be non-negative")
    @JsonSerialize(using = ToStringSerializer.class)
    @TableField("output_per_million_tokens")
    private BigDecimal outputPerMillionTokens;

    @NotNull(message = "Cached input price cannot be null")
    @DecimalMin(value = "0.0", message = "Cached input price must be non-negative")
    @JsonSerialize(using = ToStringSerializer.class)
    @TableField("cached_input_price")
    private BigDecimal cachedInputPrice;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.UPDATE)
    private OffsetDateTime updatedAt;
}
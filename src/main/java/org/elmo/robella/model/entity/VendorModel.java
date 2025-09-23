package org.elmo.robella.model.entity;

import org.elmo.robella.common.ProviderType;
import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@TableName("vendor_model")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VendorModel {
    @TableId(type = IdType.AUTO)
    private Long id;

    @NotNull(message = "Model ID cannot be null")
    private Long modelId;

    @NotBlank(message = "Model key cannot be blank")
    @TableField("model_key")
    private String modelKey;

    @NotNull(message = "Provider ID cannot be null")
    private Long providerId;

    @NotBlank(message = "Vendor model name cannot be blank")
    @TableField("vendor_model_name")
    private String vendorModelName;

    @NotBlank(message = "Vendor model key cannot be blank")
    @TableField("vendor_model_key")
    private String vendorModelKey;

    @NotNull(message = "Provider type cannot be null")
    private ProviderType providerType;

    private String description;

    @NotNull(message = "Input price per million tokens cannot be null")
    @DecimalMin(value = "0.0", message = "Input price must be non-negative")
    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal inputPerMillionTokens;

    @NotNull(message = "Output price per million tokens cannot be null")
    @DecimalMin(value = "0.0", message = "Output price must be non-negative")
    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal outputPerMillionTokens;

    @NotBlank(message = "Currency cannot be blank")
    private String currency;

    @NotNull(message = "Cached input price cannot be null")
    @DecimalMin(value = "0.0", message = "Cached input price must be non-negative")
    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal cachedInputPrice;

    @NotNull(message = "Cached output price cannot be null")
    @DecimalMin(value = "0.0", message = "Cached output price must be non-negative")
    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal cachedOutputPrice;

    @NotNull(message = "Weight cannot be null")
    @DecimalMin(value = "0.1", message = "Weight must be positive")
    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal weight = BigDecimal.valueOf(5.0);

    @NotNull(message = "Enabled status cannot be null")
    private Boolean enabled;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.UPDATE)
    private OffsetDateTime updatedAt;
}
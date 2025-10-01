package org.elmo.robella.model.dto;

import org.elmo.robella.common.ProviderType;
import org.elmo.robella.model.entity.PricingTier;
import org.elmo.robella.model.entity.VendorModel;
import org.elmo.robella.model.enums.PricingStrategyType;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VendorModelDTO {

    private Long id;

    @NotNull(message = "Model ID cannot be null")
    private Long modelId;

    @NotBlank(message = "Model key cannot be blank")
    private String modelKey;

    @NotNull(message = "Provider ID cannot be null")
    private Long providerId;

    @NotBlank(message = "Vendor model name cannot be blank")
    private String vendorModelName;

    @NotBlank(message = "Vendor model key cannot be blank")
    private String vendorModelKey;

    @NotNull(message = "Provider type cannot be null")
    private ProviderType providerType;

    private String description;

    @DecimalMin(value = "0.0", message = "Input price must be non-negative")
    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal inputPerMillionTokens;

    @DecimalMin(value = "0.0", message = "Output price must be non-negative")
    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal outputPerMillionTokens;

    @DecimalMin(value = "0.0", message = "Per request price must be non-negative")
    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal perRequestPrice;

    private String currency;

    @NotNull(message = "Cached input price cannot be null")
    @DecimalMin(value = "0.0", message = "Cached input price must be non-negative")
    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal cachedInputPrice;

    @NotNull(message = "Pricing strategy cannot be null")
    private PricingStrategyType pricingStrategy;

    @NotNull(message = "Weight cannot be null")
    @DecimalMin(value = "0.1", message = "Weight must be positive")
    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal weight = BigDecimal.valueOf(5.0);

    @NotNull(message = "Enabled status cannot be null")
    private Boolean enabled;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;

    // 阶梯计费相关字段
    private List<PricingTier> pricingTiers;

    // 静态内部类用于创建和更新请求
    public static class CreateRequest {
        private VendorModel vendorModel;
        private List<PricingTier> pricingTiers;

        // Getters and Setters
        public VendorModel getVendorModel() { return vendorModel; }
        public void setVendorModel(VendorModel vendorModel) { this.vendorModel = vendorModel; }
        public List<PricingTier> getPricingTiers() { return pricingTiers; }
        public void setPricingTiers(List<PricingTier> pricingTiers) { this.pricingTiers = pricingTiers; }
    }

    public static class UpdateRequest {
        private VendorModel vendorModel;
        private List<PricingTier> pricingTiers;

        // Getters and Setters
        public VendorModel getVendorModel() { return vendorModel; }
        public void setVendorModel(VendorModel vendorModel) { this.vendorModel = vendorModel; }
        public List<PricingTier> getPricingTiers() { return pricingTiers; }
        public void setPricingTiers(List<PricingTier> pricingTiers) { this.pricingTiers = pricingTiers; }
    }
}
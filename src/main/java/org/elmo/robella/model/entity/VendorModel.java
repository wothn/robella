package org.elmo.robella.model.entity;

import org.elmo.robella.common.ProviderType;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Table;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Table("vendor_model")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VendorModel {
    @Id
    private Long id;
    private Long modelId;
    private Long providerId;
    private String vendorModelName;
    private String vendorModelKey;
    private ProviderType providerType;
    private String description;
    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal inputPerMillionTokens;
    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal outputPerMillionTokens;
    private String currency;
    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal cachedInputPrice;
    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal cachedOutputPrice;
    private Boolean enabled;
    @CreatedDate
    private LocalDateTime createdAt;
    @LastModifiedDate
    private LocalDateTime updatedAt;
}
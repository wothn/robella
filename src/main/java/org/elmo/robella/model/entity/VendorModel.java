package org.elmo.robella.model.entity;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Table;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Table("vendor_model")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VendorModel {
    @Id
    private Long id;
    private Long modelId;
    private Long providerId;
    private String vendorModelName;
    private String description;
    private Map<String, Object> pricing;
    private Boolean enabled;
    @CreatedDate
    private LocalDateTime createdAt;
    @LastModifiedDate
    private LocalDateTime updatedAt;
}
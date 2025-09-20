package org.elmo.robella.model.entity;

import org.elmo.robella.common.EndpointType;
import org.elmo.robella.common.ProviderType;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Table;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import java.time.OffsetDateTime;

@Data
@Table("provider")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Provider {
    @Id
    private Long id;
    private String name;
    private EndpointType endpointType;
    private ProviderType providerType;
    private String baseUrl;
    private String apiKey;
    private String config;
    private Boolean enabled;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
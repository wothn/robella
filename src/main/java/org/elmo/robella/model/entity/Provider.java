package org.elmo.robella.model.entity;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Table;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import java.time.LocalDateTime;
import org.elmo.robella.model.common.EndpointType;

@Data
@Table("provider")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Provider {
    @Id
    private Long id;
    private String name;
    private EndpointType type;
    private String baseUrl;
    private String apiKey;
    private String config;
    private Boolean enabled;
    @CreatedDate
    private LocalDateTime createdAt;
    @LastModifiedDate
    private LocalDateTime updatedAt;
}
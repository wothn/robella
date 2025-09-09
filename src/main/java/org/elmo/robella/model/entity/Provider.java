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
@Table("provider")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Provider {
    @Id
    private Long id;
    private String name;
    private String type;
    private String baseUrl;
    private String apiKey;
    private Map<String, Object> config;
    private Boolean enabled;
    @CreatedDate
    private LocalDateTime createdAt;
    @LastModifiedDate
    private LocalDateTime updatedAt;
}
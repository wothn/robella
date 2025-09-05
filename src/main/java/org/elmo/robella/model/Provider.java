package org.elmo.robella.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Table("providers")
public class Provider {
    @Id
    private Long id;
    private String name;
    private String type;
    private String apiKey;
    private String baseUrl;
    private String deploymentName;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
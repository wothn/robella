package org.elmo.robella.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Table("models")
public class Model {
    @Id
    private Long id;
    private Long providerId;
    private String name;
    private String vendorModel;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
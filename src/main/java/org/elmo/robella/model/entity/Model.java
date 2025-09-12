package org.elmo.robella.model.entity;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Table;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Slf4j
@Table("model")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Model {
    @Id
    private Long id;
    private String name;
    private String description;
    private String organization;
    private List<ModelCapability> capabilities;
    private Integer contextWindow;
    private Boolean published;
    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

}
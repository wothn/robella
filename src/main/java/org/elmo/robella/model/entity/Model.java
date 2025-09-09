package org.elmo.robella.model.entity;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Table;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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
    private Map<String, Object> capabilities;
    private Integer contextWindow;
    private Boolean isPublished;
    @CreatedDate
    private LocalDateTime createdAt;
    @LastModifiedDate
    private LocalDateTime updatedAt;
    
    // Transient fields for JSON conversion
    @Transient
    private List<ModelCapability> capabilitiesList;
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    // Helper methods to convert between JSON and List<ModelCapability>
    public List<ModelCapability> getCapabilitiesList() {
        if (capabilitiesList == null && capabilities != null) {
            try {
                capabilitiesList = objectMapper.readValue(objectMapper.writeValueAsString(capabilities), 
                    objectMapper.getTypeFactory().constructCollectionType(List.class, ModelCapability.class));
            } catch (Exception e) {
                log.error("Failed to parse capabilities JSON: {}", e.getMessage());
            }
        }
        return capabilitiesList;
    }
    
    public void setCapabilitiesList(List<ModelCapability> capabilitiesList) {
        this.capabilitiesList = capabilitiesList;
        if (capabilitiesList != null) {
            try {
                this.capabilities = objectMapper.readValue(objectMapper.writeValueAsString(capabilitiesList), 
                    objectMapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class));
            } catch (Exception e) {
                log.error("Failed to serialize capabilities to JSON: {}", e.getMessage());
            }
        } else {
            this.capabilities = null;
        }
    }
}
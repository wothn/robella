package org.elmo.robella.model.dto;

import java.util.List;

import lombok.Data;

@Data
public class ProviderDTO {
    private String name;
    private String type;
    private String baseUrl;
    private String apiKey;
    private String deploymentName;
    private Boolean enabled;
    private List<ModelDTO> models;
}

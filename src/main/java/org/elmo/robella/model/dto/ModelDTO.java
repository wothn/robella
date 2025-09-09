package org.elmo.robella.model.dto;

import org.elmo.robella.model.entity.ModelCapability;
import org.elmo.robella.model.entity.Pricing;

import lombok.Data;

@Data
public class ModelDTO {
    private Long id;
    private String name;
    private String group;
    private Boolean enabled;
    private String vendorModel;
    private Pricing pricing;
    private ModelCapability capability;
}

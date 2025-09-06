package org.elmo.robella.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Slf4j
@Table("models")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Model {
    @Id
    private Long id;
    private Long providerId;
    private String name;
    
    @Column("group_name")
    private String group;
    
    @Column("owned_by")
    private String ownedBy;
    
    private String description;
    
    private String capabilities; // JSON string
    
    @Column("input_per_million_tokens")
    private Double inputPerMillionTokens;
    
    @Column("output_per_million_tokens")
    private Double outputPerMillionTokens;
    
    @Column("currency_symbol")
    private String currencySymbol = "USD";
    
    @Column("cached_input_price")
    private Double cachedInputPrice;
    
    @Column("cached_output_price")
    private Double cachedOutputPrice;
    
    @Column("supported_text_delta")
    private Boolean supportedTextDelta = false;
    
    private String vendorModel;
    private Boolean enabled = true;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Transient fields for JSON conversion
    @Transient
    private List<ModelCapability> capabilitiesList;
    @Transient
    private Pricing pricingInfo;
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    // Helper methods to convert between JSON string and List<ModelCapability>
    public List<ModelCapability> getCapabilitiesList() {
        if (capabilitiesList == null && capabilities != null && !capabilities.trim().isEmpty()) {
            try {
                capabilitiesList = objectMapper.readValue(capabilities, 
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
                this.capabilities = objectMapper.writeValueAsString(capabilitiesList);
            } catch (Exception e) {
                log.error("Failed to serialize capabilities to JSON: {}", e.getMessage());
            }
        } else {
            this.capabilities = null;
        }
    }
    
    // Helper methods to convert between separate pricing columns and Pricing object
    public Pricing getPricingInfo() {
        if (pricingInfo == null) {
            pricingInfo = new Pricing();
            pricingInfo.setInputPerMillionTokens(inputPerMillionTokens);
            pricingInfo.setOutputPerMillionTokens(outputPerMillionTokens);
            pricingInfo.setCurrencySymbol(currencySymbol);
            pricingInfo.setCachedInputPrice(cachedInputPrice);
            pricingInfo.setCachedOutputPrice(cachedOutputPrice);
        }
        return pricingInfo;
    }
    
    public void setPricingInfo(Pricing pricingInfo) {
        this.pricingInfo = pricingInfo;
        if (pricingInfo != null) {
            this.inputPerMillionTokens = pricingInfo.getInputPerMillionTokens();
            this.outputPerMillionTokens = pricingInfo.getOutputPerMillionTokens();
            this.currencySymbol = pricingInfo.getCurrencySymbol();
            this.cachedInputPrice = pricingInfo.getCachedInputPrice();
            this.cachedOutputPrice = pricingInfo.getCachedOutputPrice();
        } else {
            this.inputPerMillionTokens = null;
            this.outputPerMillionTokens = null;
            this.currencySymbol = "$";
            this.cachedInputPrice = null;
            this.cachedOutputPrice = null;
        }
    }
}
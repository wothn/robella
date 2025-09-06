package org.elmo.robella.model;

import lombok.Data;

@Data
public class Pricing {
    private Double inputPerMillionTokens;
    private Double outputPerMillionTokens;
    private String currencySymbol = "USD";
    
    // Cache prices
    private Double cachedInputPrice;
    private Double cachedOutputPrice;
}
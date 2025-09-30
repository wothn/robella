package org.elmo.robella.service.pricing;

import org.elmo.robella.model.entity.VendorModel;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class FixedPricingStrategy implements PricingStrategy {
    
    private final VendorModel vendorModel;
    
    public FixedPricingStrategy(VendorModel vendorModel) {
        if (vendorModel == null) {
            throw new IllegalArgumentException("Vendor model cannot be null");
        }
        this.vendorModel = vendorModel;
    }
    
    @Override
    public BigDecimal calculateInputCost(long inputTokens, long cachedTokens) {
        BigDecimal totalCost = BigDecimal.ZERO;
        
        // 计算缓存部分的成本
        if (cachedTokens > 0) {
            BigDecimal cachedCost = BigDecimal.valueOf(cachedTokens)
                .multiply(vendorModel.getCachedInputPrice())
                .divide(BigDecimal.valueOf(1_000_000), 10, RoundingMode.HALF_UP);
            totalCost = totalCost.add(cachedCost);
        }
        
        // 计算非缓存部分的成本
        long nonCachedTokens = inputTokens - cachedTokens;
        if (nonCachedTokens > 0) {
            BigDecimal nonCachedCost = BigDecimal.valueOf(nonCachedTokens)
                .multiply(vendorModel.getInputPerMillionTokens())
                .divide(BigDecimal.valueOf(1_000_000), 10, RoundingMode.HALF_UP);
            totalCost = totalCost.add(nonCachedCost);
        }
        
        return totalCost.setScale(6, RoundingMode.HALF_UP);
    }
    
    @Override
    public BigDecimal calculateOutputCost(long outputTokens) {
        return BigDecimal.valueOf(outputTokens)
            .multiply(vendorModel.getOutputPerMillionTokens())
            .divide(BigDecimal.valueOf(1_000_000), 6, RoundingMode.HALF_UP);
    }
    
    @Override
    public String getCurrency() {
        return vendorModel.getCurrency();
    }
}
package org.elmo.robella.service.pricing;

import org.elmo.robella.model.entity.VendorModel;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class PerRequestPricingStrategy implements PricingStrategy {
    
    private final VendorModel vendorModel;
    
    public PerRequestPricingStrategy(VendorModel vendorModel) {
        if (vendorModel == null) {
            throw new IllegalArgumentException("Vendor model cannot be null");
        }
        this.vendorModel = vendorModel;
    }
    
    @Override
    public BigDecimal calculateInputCost(long inputTokens, long cachedTokens) {
        // 按请求次数计费，输入成本为0，因为总成本在calculateTotalCost中统一计算
        return BigDecimal.ZERO;
    }
    
    @Override
    public BigDecimal calculateOutputCost(long outputTokens) {
        // 按请求次数计费，输出成本为0，因为总成本在calculateTotalCost中统一计算
        return BigDecimal.ZERO;
    }
    
    @Override
    public BigDecimal calculateTotalCost(long inputTokens, long cachedTokens, long outputTokens) {
        // 按请求次数计费，只计算一次固定价格
        // 使用inputPerMillionTokens作为每次请求的固定价格
        return vendorModel.getInputPerMillionTokens()
            .setScale(6, RoundingMode.HALF_UP);
    }
    
    @Override
    public String getCurrency() {
        return vendorModel.getCurrency();
    }
}
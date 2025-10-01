package org.elmo.robella.service.pricing;

import lombok.extern.slf4j.Slf4j;
import org.elmo.robella.model.entity.VendorModel;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
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
        // 使用perRequestPrice作为每次请求的固定价格，如果为null则回退到inputPerMillionTokens
        BigDecimal perRequestPrice = vendorModel.getPerRequestPrice();
        if (perRequestPrice != null) {
            return perRequestPrice.setScale(6, RoundingMode.HALF_UP);
        } else {
            // 回退方案：使用inputPerMillionTokens作为每次请求的固定价格
            log.warn("PerRequestPrice is null, using InputPerMillionTokens as fallback for vendor model {}",
                    vendorModel.getId());
            return vendorModel.getInputPerMillionTokens()
                .setScale(6, RoundingMode.HALF_UP);
        }
    }
    
    @Override
    public String getCurrency() {
        return vendorModel.getCurrency();
    }
}
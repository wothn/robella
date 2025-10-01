package org.elmo.robella.service.pricing;

import org.elmo.robella.mapper.PricingTierMapper;
import org.elmo.robella.model.entity.PricingTier;
import org.elmo.robella.model.entity.VendorModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import org.elmo.robella.model.enums.PricingStrategyType;

import java.util.List;

@Service
public class PricingStrategyFactory {
    
    @Autowired
    private PricingTierMapper pricingTierMapper;
    
    /**
     * 根据供应商模型创建相应的计费策略
     * @param vendorModel 供应商模型
     * @return 计费策略实例
     */
    public PricingStrategy createPricingStrategy(VendorModel vendorModel) {
        if (vendorModel == null) {
            throw new IllegalArgumentException("Vendor model cannot be null");
        }
        
        PricingStrategyType strategy = vendorModel.getPricingStrategy();
        if (strategy == null) {
            strategy = PricingStrategyType.FIXED;
        }
        
        switch (strategy) {
            case TIERED:
                return createTieredPricingStrategy(vendorModel);
            case PER_REQUEST:
                return new PerRequestPricingStrategy(vendorModel);
            case FIXED:
            default:
                return new FixedPricingStrategy(vendorModel);
        }
    }
    
    private PricingStrategy createTieredPricingStrategy(VendorModel vendorModel) {
        
        LambdaQueryWrapper<PricingTier> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PricingTier::getVendorModelId, vendorModel.getId());
        List<PricingTier> pricingTiers = pricingTierMapper.selectList(queryWrapper);

        if (pricingTiers == null || pricingTiers.isEmpty()) {
            // 如果没有配置阶梯价格，回退到固定价格
            return new FixedPricingStrategy(vendorModel);
        }
        
        return new TieredPricingStrategy(pricingTiers, vendorModel);
    }
}
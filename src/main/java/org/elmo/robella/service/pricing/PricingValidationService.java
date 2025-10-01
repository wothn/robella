package org.elmo.robella.service.pricing;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elmo.robella.model.entity.PricingTier;
import org.elmo.robella.model.entity.VendorModel;
import org.elmo.robella.model.enums.PricingStrategyType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PricingValidationService {
    
    /**
     * 验证定价阶梯配置的完整性和一致性
     */
    public void validatePricingTiers(List<PricingTier> tiers) {
        if (tiers == null || tiers.isEmpty()) {
            throw new IllegalArgumentException("Pricing tiers cannot be empty");
        }
        
        // 按层级编号排序
        tiers.sort((a, b) -> Integer.compare(a.getTierNumber(), b.getTierNumber()));
        
        // 验证每个阶梯
        for (int i = 0; i < tiers.size(); i++) {
            PricingTier tier = tiers.get(i);
            
            // 验证基本字段
            if (tier.getTierNumber() <= 0) {
                throw new IllegalArgumentException("Tier number must be positive");
            }
            
            if (tier.getMinTokens() < 0) {
                throw new IllegalArgumentException("Min tokens cannot be negative");
            }
            
            if (tier.getMaxTokens() <= tier.getMinTokens()) {
                throw new IllegalArgumentException("Max tokens must be greater than min tokens");
            }
            
            if (tier.getInputPerMillionTokens().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Input price cannot be negative");
            }
            
            if (tier.getOutputPerMillionTokens().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Output price cannot be negative");
            }
            
            if (tier.getCachedInputPrice().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Cached input price cannot be negative");
            }
            
            
            // 验证阶梯间的连续性
            if (i > 0) {
                PricingTier prevTier = tiers.get(i - 1);
                if (tier.getMinTokens() != prevTier.getMaxTokens() + 1) {
                    throw new IllegalArgumentException(
                        String.format("Tier %d min tokens (%d) must be exactly one more than previous tier max tokens (%d)",
                            tier.getTierNumber(), tier.getMinTokens(), prevTier.getMaxTokens())
                    );
                }
            } else {
                // 第一个阶梯必须从0开始
                if (tier.getMinTokens() != 0) {
                    throw new IllegalArgumentException("First tier must start from 0 tokens");
                }
            }
        }
        
        log.info("Pricing tiers validation passed for {} tiers", tiers.size());
    }
    
    /**
     * 验证供应商模型的定价配置
     */
    public void validateVendorModelPricing(VendorModel vendorModel) {
        if (vendorModel == null) {
            throw new IllegalArgumentException("Vendor model cannot be null");
        }
        
        if (vendorModel.getPricingStrategy() == null) {
            throw new IllegalArgumentException("Pricing strategy cannot be null");
        }
        
        // 固定价格模式验证
        if (vendorModel.getPricingStrategy() == PricingStrategyType.FIXED) {
            if (vendorModel.getInputPerMillionTokens().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Input price per million tokens cannot be negative");
            }
            
            if (vendorModel.getOutputPerMillionTokens().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Output price per million tokens cannot be negative");
            }
            
            if (vendorModel.getCachedInputPrice().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Cached input price cannot be negative");
            }
        }
        
        log.info("Vendor model pricing validation passed for strategy: {}", vendorModel.getPricingStrategy());
    }
    
    /**
     * 验证令牌数量参数
     */
    public void validateTokenCounts(long inputTokens, long cachedTokens, long outputTokens) {
        if (inputTokens < 0) {
            throw new IllegalArgumentException("Input tokens cannot be negative");
        }
        
        if (cachedTokens < 0) {
            throw new IllegalArgumentException("Cached tokens cannot be negative");
        }
        
        if (outputTokens < 0) {
            throw new IllegalArgumentException("Output tokens cannot be negative");
        }
        
        if (cachedTokens > inputTokens) {
            throw new IllegalArgumentException("Cached tokens cannot exceed input tokens");
        }
        
        // 合理的上限检查（防止溢出）
        long totalTokens = inputTokens + outputTokens;
        if (totalTokens > 1_000_000_000_000L) { // 1万亿令牌
            throw new IllegalArgumentException("Total tokens exceed reasonable limit");
        }
    }
    
    /**
     * 验证价格参数的合理性
     */
    public void validatePriceRange(BigDecimal price, String fieldName) {
        if (price == null) {
            throw new IllegalArgumentException(fieldName + " cannot be null");
        }
        
        if (price.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(fieldName + " cannot be negative");
        }
        
        // 合理的上限检查（防止异常高的价格）
        if (price.compareTo(new BigDecimal("1000")) > 0) {
            log.warn("{} price {} seems unusually high", fieldName, price);
        }
    }
    
    /**
     * 检查阶梯配置是否存在重叠或间隙
     */
    public void checkTierCoverage(List<PricingTier> tiers) {
        if (tiers.isEmpty()) {
            return;
        }
        
        // 检查是否从0开始
        if (tiers.get(0).getMinTokens() != 0) {
            log.warn("First tier does not start from 0 tokens, potential gap in coverage");
        }
        
        // 检查是否有无限覆盖的最后一个阶梯
        PricingTier lastTier = tiers.get(tiers.size() - 1);
        if (lastTier.getMaxTokens() != Long.MAX_VALUE) {
            log.warn("Last tier has finite max tokens ({}), potential gap in coverage for high usage", lastTier.getMaxTokens());
        }
    }
}
package org.elmo.robella.service.pricing;

import org.elmo.robella.model.entity.PricingTier;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class TieredPricingStrategy implements PricingStrategy {
    
    private final List<PricingTier> pricingTiers;
    private final String currency;
    
    public TieredPricingStrategy(List<PricingTier> pricingTiers) {
        if (pricingTiers == null || pricingTiers.isEmpty()) {
            throw new IllegalArgumentException("Pricing tiers cannot be null or empty");
        }
        this.pricingTiers = pricingTiers.stream()
            .sorted((a, b) -> Integer.compare(a.getTierNumber(), b.getTierNumber()))
            .toList();
        this.currency = pricingTiers.get(0).getCurrency();
    }
    
    @Override
    public BigDecimal calculateInputCost(long inputTokens, long cachedTokens) {
        if (inputTokens <= 0) {
            return BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP);
        }
        
        // 固定阶梯定价：所有输入令牌都按相同的阶梯价格计算
        // 根据总输入令牌数量确定适用的阶梯
        BigDecimal totalCost;
        
        if (cachedTokens > 0) {
            // 分别计算缓存和非缓存令牌的成本，但都按相同的阶梯价格计算
            BigDecimal cachedCost = calculateCostForTokens(cachedTokens, true, true, inputTokens);
            BigDecimal nonCachedCost = calculateCostForTokens(inputTokens - cachedTokens, true, false, inputTokens);
            totalCost = cachedCost.add(nonCachedCost);
        } else {
            // 没有缓存令牌，直接计算总成本
            totalCost = calculateCostForTokens(inputTokens, true, false, inputTokens);
        }
        
        return totalCost.setScale(6, RoundingMode.HALF_UP);
    }
    
    @Override
    public BigDecimal calculateOutputCost(long outputTokens) {
        return calculateCostForTokens(outputTokens, false, false, outputTokens)
            .setScale(6, RoundingMode.HALF_UP);
    }
    
    private BigDecimal calculateCostForTokens(long tokens, boolean isInput, boolean isCached, long totalTokensForTier) {
        if (tokens <= 0) {
            return BigDecimal.ZERO;
        }
        
        // 使用总令牌数量来确定适用的阶梯
        // 固定阶梯定价：一旦达到某个阶梯，所有令牌都按该阶梯价格计算
        long tokensForTier = Math.max(tokens, totalTokensForTier);
        
        for (PricingTier tier : pricingTiers) {
            long tierMinTokens = tier.getMinTokens();
            Long tierMaxTokens = tier.getMaxTokens();
            
            // 检查总令牌数量是否在当前阶梯范围内
            boolean inTier = tokensForTier >= tierMinTokens && 
                            (tierMaxTokens == null || tokensForTier <= tierMaxTokens);
            
            if (inTier) {
                // 获取当前阶梯的价格
                BigDecimal pricePerMillion;
                if (isInput) {
                    pricePerMillion = isCached ? tier.getCachedInputPrice() : tier.getInputPerMillionTokens();
                } else {
                    pricePerMillion = tier.getOutputPerMillionTokens();
                }
                
                // 计算成本：所有令牌都按该阶梯价格计算
                return BigDecimal.valueOf(tokens)
                    .multiply(pricePerMillion)
                    .divide(BigDecimal.valueOf(1_000_000), 10, RoundingMode.HALF_UP);
            }
        }
        
        // 如果没有找到适用的阶梯，使用最后一个阶梯的价格
        PricingTier lastTier = pricingTiers.get(pricingTiers.size() - 1);
        BigDecimal pricePerMillion;
        if (isInput) {
            pricePerMillion = isCached ? lastTier.getCachedInputPrice() : lastTier.getInputPerMillionTokens();
        } else {
            pricePerMillion = lastTier.getOutputPerMillionTokens();
        }
        
        return BigDecimal.valueOf(tokens)
            .multiply(pricePerMillion)
            .divide(BigDecimal.valueOf(1_000_000), 10, RoundingMode.HALF_UP);
    }
    
    @Override
    public String getCurrency() {
        return currency;
    }
}
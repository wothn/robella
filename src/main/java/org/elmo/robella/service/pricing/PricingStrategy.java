package org.elmo.robella.service.pricing;

import java.math.BigDecimal;

public interface PricingStrategy {
    
    /**
     * 计算输入令牌的成本
     * @param inputTokens 输入令牌数量
     * @param cachedTokens 缓存的输入令牌数量
     * @return 输入成本
     */
    BigDecimal calculateInputCost(long inputTokens, long cachedTokens);
    
    /**
     * 计算输出令牌的成本
     * @param outputTokens 输出令牌数量
     * @return 输出成本
     */
    BigDecimal calculateOutputCost(long outputTokens);
    
    /**
     * 获取总成本
     * @param inputTokens 输入令牌数量
     * @param cachedTokens 缓存的输入令牌数量  
     * @param outputTokens 输出令牌数量
     * @return 总成本
     */
    default BigDecimal calculateTotalCost(long inputTokens, long cachedTokens, long outputTokens) {
        BigDecimal inputCost = calculateInputCost(inputTokens, cachedTokens);
        BigDecimal outputCost = calculateOutputCost(outputTokens);
        return inputCost.add(outputCost);
    }
    
    /**
     * 获取货币类型
     * @return 货币代码
     */
    String getCurrency();
}
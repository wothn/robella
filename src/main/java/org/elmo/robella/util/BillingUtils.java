package org.elmo.robella.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elmo.robella.context.RequestContextHolder;
import org.elmo.robella.model.entity.VendorModel;
import org.elmo.robella.model.openai.core.Usage;
import org.elmo.robella.service.ExchangeRateService;
import org.elmo.robella.model.enums.PricingStrategyType;
import org.elmo.robella.service.pricing.PricingStrategyFactory;
import org.elmo.robella.service.pricing.PricingStrategy;
import org.elmo.robella.service.pricing.PricingValidationService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@Service
@RequiredArgsConstructor
public class BillingUtils {
    
    private final ExchangeRateService exchangeRateService;
    private final PricingStrategyFactory pricingStrategyFactory;
    private final PricingValidationService validationService;

    /**
     * 计算请求成本
     * @param usage 使用统计信息
     * @return BillingResult 包含输入成本、输出成本和总成本
     */
    public BillingResult calculateCost(Usage usage) {
        RequestContextHolder.RequestContext context = RequestContextHolder.getContext();
        if (context == null || context.getVendorModel() == null) {
            log.warn("No vendor model found in context for billing calculation");
            return new BillingResult(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, "USD");
        }

        VendorModel vendorModel = context.getVendorModel();
        
        // 使用计费策略工厂获取合适的计费策略
        PricingStrategy pricingStrategy = pricingStrategyFactory.createPricingStrategy(vendorModel);
        
        // 获取输入令牌数量
        long inputTokens = 0;
        long cachedTokens = 0;
        
        if (usage.getPromptCacheHitTokens() != null && usage.getPromptCacheMissTokens() != null) {
            // 有详细的缓存信息
            inputTokens = usage.getPromptCacheHitTokens() + usage.getPromptCacheMissTokens();
            cachedTokens = usage.getPromptCacheHitTokens();
        } else if (usage.getPromptTokens() != null) {
            // 没有详细缓存信息，全部视为非缓存
            inputTokens = usage.getPromptTokens();
            cachedTokens = 0;
        }
        
        // 获取输出令牌数量
        long outputTokens = usage.getCompletionTokens() != null ? usage.getCompletionTokens() : 0;
        
        // 使用验证服务验证令牌数量
        validationService.validateTokenCounts(inputTokens, cachedTokens, outputTokens);
        
        // 如果是按次计费，只计算一次总成本
        if (vendorModel.getPricingStrategy() == PricingStrategyType.PER_REQUEST) {
            BigDecimal totalCost = pricingStrategy.calculateTotalCost(inputTokens, cachedTokens, outputTokens);
            BigDecimal totalCostCNY = exchangeRateService.convertToCNY(totalCost, pricingStrategy.getCurrency());
            return new BillingResult(BigDecimal.ZERO, BigDecimal.ZERO, totalCostCNY, "CNY");
        }
        
        // 使用计费策略计算成本（原有的令牌计费逻辑）
        BigDecimal inputCost = pricingStrategy.calculateInputCost(inputTokens, cachedTokens);
        BigDecimal outputCost = pricingStrategy.calculateOutputCost(outputTokens);
        BigDecimal totalCost = inputCost.add(outputCost);
        
        log.debug("Cost calculation: strategy={}, inputTokens={}, cachedTokens={}, outputTokens={}, inputCost={}, outputCost={}, totalCost={}",
                 vendorModel.getPricingStrategy(), inputTokens, cachedTokens, outputTokens, inputCost, outputCost, totalCost);
        
        // 将所有成本转换为CNY
        BigDecimal inputCostCNY = exchangeRateService.convertToCNY(inputCost, pricingStrategy.getCurrency());
        BigDecimal outputCostCNY = exchangeRateService.convertToCNY(outputCost, pricingStrategy.getCurrency());
        BigDecimal totalCostCNY = exchangeRateService.convertToCNY(totalCost, pricingStrategy.getCurrency());
        
        return new BillingResult(inputCostCNY, outputCostCNY, totalCostCNY, "CNY");
    }

    /**
     * 计费结果记录类
     */
    public record BillingResult(
        BigDecimal inputCost,
        BigDecimal outputCost,
        BigDecimal totalCost,
        String currency
    ) {}
}
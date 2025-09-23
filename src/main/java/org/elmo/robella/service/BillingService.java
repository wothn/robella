package org.elmo.robella.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elmo.robella.context.RequestContextHolder;
import org.elmo.robella.model.entity.VendorModel;
import org.elmo.robella.model.openai.core.Usage;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@Service
@RequiredArgsConstructor
public class BillingService {

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
        BigDecimal inputCost = BigDecimal.ZERO;
        BigDecimal outputCost = BigDecimal.ZERO;

        // 计算输入成本：区分缓存和正常token
        if (usage.getPromptCacheHitTokens() != null && usage.getPromptCacheMissTokens() != null) {
            // 有详细的缓存信息，分别计算
            BigDecimal cachedInputCost = calculateTokenCost(usage.getPromptCacheHitTokens(), vendorModel.getCachedInputPrice());
            BigDecimal normalInputCost = calculateTokenCost(usage.getPromptCacheMissTokens(), vendorModel.getInputPerMillionTokens());
            inputCost = cachedInputCost.add(normalInputCost);
            log.debug("Mixed billing: cached={} tokens @ ${}, normal={} tokens @ ${}",
                     usage.getPromptCacheHitTokens(), vendorModel.getCachedInputPrice(),
                     usage.getPromptCacheMissTokens(), vendorModel.getInputPerMillionTokens());
        } else if (usage.getPromptTokens() != null) {
            // 没有详细缓存信息，使用正常价格
            inputCost = calculateTokenCost(usage.getPromptTokens(), vendorModel.getInputPerMillionTokens());
        }

        // 计算输出成本（输出token没有缓存）
        outputCost = usage.getCompletionTokens() != null ?
            calculateTokenCost(usage.getCompletionTokens(), vendorModel.getOutputPerMillionTokens()) : BigDecimal.ZERO;

        BigDecimal totalCost = inputCost.add(outputCost);
        return new BillingResult(inputCost, outputCost, totalCost, vendorModel.getCurrency());
    }

    /**
     * 计算token成本
     * @param tokens token数量
     * @param pricePerMillionTokens 每百万token价格
     * @return 计算后的成本
     */
    private BigDecimal calculateTokenCost(int tokens, BigDecimal pricePerMillionTokens) {
        if (tokens <= 0 || pricePerMillionTokens.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        // 成本 = (token数量 / 1,000,000) * 每百万token价格
        return BigDecimal.valueOf(tokens)
                .divide(BigDecimal.valueOf(1_000_000), 10, RoundingMode.HALF_UP)
                .multiply(pricePerMillionTokens)
                .setScale(6, RoundingMode.HALF_UP);
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
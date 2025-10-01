package org.elmo.robella.service.loadblancer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.elmo.robella.model.entity.PricingTier;
import org.elmo.robella.model.entity.VendorModel;
import org.elmo.robella.model.enums.PricingStrategyType;
import org.elmo.robella.service.PricingTierService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "robella.loadbalancer.strategy", havingValue = "HYBRID_WEIGHTED")
public class HybridWeightedLoadBalancer implements LoadBalancerStrategy {

    private static final int COST_SCALE = 10;

    private final PricingTierService pricingTierService;

    @Override
    public VendorModel select(List<VendorModel> candidates) {
        if (candidates.isEmpty()) {
            return null;
        }

        // 计算加权候选列表
        List<WeightedCandidate> weightedCandidates = calculateWeightedCandidates(candidates);

        // 基于权重随机选择
        return selectByWeight(weightedCandidates);
    }

    private List<WeightedCandidate> calculateWeightedCandidates(List<VendorModel> candidates) {
        return candidates.stream()
                .map(vm -> {
                    BigDecimal qualityWeight = vm.getWeight();
                    BigDecimal costWeight = calculateCostWeight(vm);
                    BigDecimal totalWeight = qualityWeight.multiply(costWeight);

                    log.debug("Model {} - Quality: {}, Cost: {}, Total: {}",
                            vm.getVendorModelName(), qualityWeight, costWeight, totalWeight);

                    return new WeightedCandidate(vm, totalWeight);
                })
                .collect(Collectors.toList());
    }

    private BigDecimal calculateCostWeight(VendorModel vm) {
        BigDecimal avgCost = resolveAverageCost(vm);

        if (avgCost == null || avgCost.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ONE;
        }

        return BigDecimal.ONE.divide(avgCost, COST_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal resolveAverageCost(VendorModel vendorModel) {
        BigDecimal avgFromModel = averageNonNullCosts(vendorModel.getInputPerMillionTokens(),
                vendorModel.getOutputPerMillionTokens());
        if (avgFromModel != null) {
            return avgFromModel;
        }

        if (vendorModel.getPricingStrategy() == PricingStrategyType.PER_REQUEST
                && vendorModel.getPerRequestPrice() != null) {
            return vendorModel.getPerRequestPrice();
        }

        if (vendorModel.getPricingStrategy() == PricingStrategyType.TIERED && vendorModel.getId() != null) {
            List<PricingTier> tiers = pricingTierService.getPricingTiersByVendorModel(vendorModel.getId());
            return averageCostFromTier(tiers);
        }

        return null;
    }

    private BigDecimal averageCostFromTier(List<PricingTier> tiers) {
        if (tiers == null || tiers.isEmpty()) {
            return null;
        }

        PricingTier firstTier = tiers.stream()
                .min(Comparator.comparing(PricingTier::getTierNumber))
                .orElse(null);

        if (firstTier == null) {
            return null;
        }

        return averageNonNullCosts(firstTier.getInputPerMillionTokens(), firstTier.getOutputPerMillionTokens());
    }

    private BigDecimal averageNonNullCosts(BigDecimal inputPrice, BigDecimal outputPrice) {
        BigDecimal sum = BigDecimal.ZERO;
        int count = 0;

        if (inputPrice != null) {
            sum = sum.add(inputPrice);
            count++;
        }

        if (outputPrice != null) {
            sum = sum.add(outputPrice);
            count++;
        }

        if (count == 0) {
            return null;
        }

        return sum.divide(BigDecimal.valueOf(count), COST_SCALE, RoundingMode.HALF_UP);
    }

    private VendorModel selectByWeight(List<WeightedCandidate> weightedCandidates) {
        // 计算总权重
        BigDecimal totalWeight = weightedCandidates.stream()
                .map(WeightedCandidate::getWeight)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalWeight.compareTo(BigDecimal.ZERO) <= 0) {
            // 如果所有权重都是0，使用随机选择
            int randomIndex = (int) (Math.random() * weightedCandidates.size());
            return weightedCandidates.get(randomIndex).getVendorModel();
        }

        // 基于权重随机选择
        double randomValue = Math.random() * totalWeight.doubleValue();
        double cumulativeWeight = 0;

        for (WeightedCandidate wc : weightedCandidates) {
            cumulativeWeight += wc.getWeight().doubleValue();
            if (randomValue <= cumulativeWeight) {
                return wc.getVendorModel();
            }
        }

        // 兜底返回最后一个
        return weightedCandidates.get(weightedCandidates.size() - 1).getVendorModel();
    }

    @Data
    @AllArgsConstructor
    private static class WeightedCandidate {
        private final VendorModel vendorModel;
        private final BigDecimal weight;
    }
}
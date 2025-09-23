package org.elmo.robella.service.loadblancer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

import org.elmo.robella.model.entity.VendorModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "robella.loadbalancer.strategy", havingValue = "HYBRID_WEIGHTED")
public class HybridWeightedLoadBalancer implements LoadBalancerStrategy {

    // 移除轮询索引，改为纯加权随机

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
        BigDecimal avgCost = vm.getInputPerMillionTokens()
            .add(vm.getOutputPerMillionTokens())
            .divide(BigDecimal.valueOf(2), 10, RoundingMode.HALF_UP);

        if (avgCost.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ONE;
        }

        return BigDecimal.ONE.divide(avgCost, 10, RoundingMode.HALF_UP);
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
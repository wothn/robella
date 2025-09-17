package org.elmo.robella.service.loadblancer;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.elmo.robella.model.entity.VendorModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "robella.loadbalancer.strategy", havingValue = "ROUND_ROBIN")
public class RoundRobinLoadBalancer implements LoadBalancerStrategy {

    private Map<Long, AtomicInteger> modelRoundRobinIndex = new ConcurrentHashMap<>();
    
    public VendorModel select(List<VendorModel> candidates) {
        if (candidates.isEmpty()) {
            return null;
        }
        // 列表的所有都属于同一个模型
        Long modelId = candidates.get(0).getModelId();

        // Get the current index for the model
        AtomicInteger currentIndex = modelRoundRobinIndex.computeIfAbsent(modelId, key -> new AtomicInteger(0));

        int size = candidates.size();
        int next = currentIndex.getAndIncrement() % size;
        if (next < 0) {
            next = 0; // 防止负数
        }
        return candidates.get(next);
    }
    
}

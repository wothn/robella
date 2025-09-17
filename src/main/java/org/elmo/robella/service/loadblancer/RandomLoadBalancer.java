package org.elmo.robella.service.loadblancer;

import java.util.List;

import org.elmo.robella.model.entity.VendorModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "robella.loadbalancer.strategy", havingValue = "RANDOM")
public class RandomLoadBalancer implements LoadBalancerStrategy {
    @Override
    public VendorModel select(List<VendorModel> candidates) {
        if (candidates.isEmpty()) {
            return null;
        }
        int randomIndex = (int) (Math.random() * candidates.size());
        return candidates.get(randomIndex);
    }
}

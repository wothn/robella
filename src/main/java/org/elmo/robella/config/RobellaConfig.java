package org.elmo.robella.config;

import lombok.Data;

import org.elmo.robella.service.loadblancer.HybridWeightedLoadBalancer;
import org.elmo.robella.service.loadblancer.LoadBalancerStrategy;
import org.elmo.robella.service.loadblancer.RoundRobinLoadBalancer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "robella")
public class RobellaConfig {

    @Bean
    @ConditionalOnMissingBean(LoadBalancerStrategy.class)
    public LoadBalancerStrategy defaultLoadBalancer() {
        return new RoundRobinLoadBalancer();
    }
}
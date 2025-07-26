package org.elmo.robella.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
public class MetricsController {

    @GetMapping("/metrics")
    public Mono<Map<String, Object>> metrics() {
        // 简单示例，实际实现中会从监控服务获取数据
        return Mono.just(Map.of(
            "uptime", System.currentTimeMillis(),
            "status", "running"
        ));
    }
}
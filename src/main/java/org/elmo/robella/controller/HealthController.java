package org.elmo.robella.controller;

import lombok.RequiredArgsConstructor;
import org.elmo.robella.service.ForwardingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class HealthController {
    
    private final ForwardingService forwardingService;

    @GetMapping("/health")
    public Mono<Map<String, String>> health() {
        return Mono.just(Map.of("status", "UP"));
    }
    
    @PostMapping("/admin/refresh-models")
    public Mono<Map<String, String>> refreshModels() {
        try {
            forwardingService.refreshModelCache();
            return Mono.just(Map.of(
                "status", "success", 
                "message", "Model cache refreshed successfully"
            ));
        } catch (Exception e) {
            return Mono.just(Map.of(
                "status", "error", 
                "message", "Failed to refresh model cache: " + e.getMessage()
            ));
        }
    }
}
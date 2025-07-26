package org.elmo.robella.service;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public interface MonitoringService {
    /**
     * 记录调用开始
     */
    String startCall(String provider, String model);

    /**
     * 记录调用结束
     */
    void endCall(String traceId, boolean success);

    /**
     * 获取统计信息
     */
    Mono<String> getStats();
}
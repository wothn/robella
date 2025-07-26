package org.elmo.robella.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
@RequiredArgsConstructor
public class MonitoringServiceImpl implements MonitoringService {

    private final MeterRegistry meterRegistry;
    private final ReactiveRedisTemplate<String, Object> redisTemplate;
    
    // 存储正在进行的调用
    private final ConcurrentHashMap<String, CallInfo> activeCalls = new ConcurrentHashMap<>();
    
    // 统计计数器
    private final AtomicLong totalCalls = new AtomicLong(0);
    private final AtomicLong successfulCalls = new AtomicLong(0);
    private final AtomicLong failedCalls = new AtomicLong(0);

    @Override
    public String startCall(String provider, String model) {
        String traceId = UUID.randomUUID().toString();
        CallInfo callInfo = new CallInfo(provider, model, System.currentTimeMillis());
        activeCalls.put(traceId, callInfo);
        totalCalls.incrementAndGet();
        return traceId;
    }

    @Override
    public void endCall(String traceId, boolean success) {
        CallInfo callInfo = activeCalls.remove(traceId);
        if (callInfo != null) {
            long duration = System.currentTimeMillis() - callInfo.startTime;
            
            // 更新计数器
            if (success) {
                successfulCalls.incrementAndGet();
            } else {
                failedCalls.incrementAndGet();
            }
            
            // 记录指标
            Timer.Sample sample = Timer.start(meterRegistry);
            sample.stop(Timer.builder("ai.provider.call.duration")
                    .tag("provider", callInfo.provider)
                    .tag("model", callInfo.model)
                    .tag("success", String.valueOf(success))
                    .register(meterRegistry));
            
            // 记录到Redis
            String key = "call:" + callInfo.provider + ":" + callInfo.model;
            redisTemplate.opsForValue().increment(key).subscribe();
            redisTemplate.opsForValue().increment(key + ":duration", duration).subscribe();
            if (!success) {
                redisTemplate.opsForValue().increment(key + ":failed").subscribe();
            }
        }
    }

    @Override
    public Mono<String> getStats() {
        return Mono.just(String.format(
            "Total calls: %d, Successful: %d, Failed: %d, Success rate: %.2f%%",
            totalCalls.get(),
            successfulCalls.get(),
            failedCalls.get(),
            totalCalls.get() > 0 ? (successfulCalls.get() * 100.0 / totalCalls.get()) : 0
        ));
    }

    private static class CallInfo {
        private final String provider;
        private final String model;
        private final long startTime;

        public CallInfo(String provider, String model, long startTime) {
            this.provider = provider;
            this.model = model;
            this.startTime = startTime;
        }
    }
}
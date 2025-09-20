package org.elmo.robella.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elmo.robella.model.entity.RequestLog;
import org.elmo.robella.repository.RequestLogRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class RequestLogService {

    private final RequestLogRepository requestLogRepository;

    public Mono<RequestLog> createRequestLog(RequestLog requestLog) {
        return requestLogRepository.save(requestLog)
                .doOnSuccess(savedLog -> log.debug("Request log saved: {}", savedLog.getId()))
                .doOnError(error -> log.error("Failed to save request log: {}", error.getMessage()));
    }

    public Flux<RequestLog> getUserLogs(Long userId) {
        return requestLogRepository.findByUserId(userId);
    }

    public Flux<RequestLog> getUserLogsBetweenDates(Long userId, OffsetDateTime startTime, OffsetDateTime endTime) {
        return requestLogRepository.findByUserIdAndCreatedAtBetween(userId, startTime, endTime);
    }

    public Flux<RequestLog> getApiKeyLogs(Long apiKeyId) {
        return requestLogRepository.findByApiKeyId(apiKeyId);
    }

    public Flux<RequestLog> getRecentLogs() {
        return requestLogRepository.findTop100ByOrderByCreatedAtDesc();
    }

    public Mono<Long> getUserRequestCount(Long userId, OffsetDateTime startTime, OffsetDateTime endTime) {
        return requestLogRepository.countByUserIdAndCreatedAtBetween(userId, startTime, endTime);
    }

    public Mono<Long> getUserTotalTokens(Long userId, OffsetDateTime startTime, OffsetDateTime endTime) {
        return requestLogRepository.sumTotalTokensByUserIdAndCreatedAtBetween(userId, startTime, endTime);
    }

    public Mono<BigDecimal> getUserTotalCost(Long userId, OffsetDateTime startTime, OffsetDateTime endTime) {
        return requestLogRepository.sumTotalCostByUserIdAndCreatedAtBetween(userId, startTime, endTime);
    }

}
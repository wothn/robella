package org.elmo.robella.repository;

import org.elmo.robella.model.entity.RequestLog;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;

public interface RequestLogRepository extends R2dbcRepository<RequestLog, Long> {

    Flux<RequestLog> findByUserId(Long userId);

    Flux<RequestLog> findByUserIdAndCreatedAtBetween(Long userId, OffsetDateTime startTime, OffsetDateTime endTime);

    Flux<RequestLog> findByApiKeyId(Long apiKeyId);

    Flux<RequestLog> findByProviderId(Long providerId);

    Flux<RequestLog> findByModelKey(String modelKey);

    Flux<RequestLog> findByEndpointType(String endpointType);

    Flux<RequestLog> findByIsSuccess(Boolean isSuccess);

    Flux<RequestLog> findByCreatedAtBetween(OffsetDateTime startTime, OffsetDateTime endTime);

    Flux<RequestLog> findByUserIdAndIsSuccess(Long userId, Boolean isSuccess);

    Mono<Long> countByUserIdAndCreatedAtBetween(Long userId, OffsetDateTime startTime, OffsetDateTime endTime);

    @Query("SELECT COALESCE(SUM(total_tokens), 0) FROM request_log WHERE user_id = :userId AND created_at BETWEEN :startTime AND :endTime")
    Mono<Long> sumTotalTokensByUserIdAndCreatedAtBetween(Long userId, OffsetDateTime startTime, OffsetDateTime endTime);

    @Query("SELECT COALESCE(SUM(total_cost), 0) FROM request_log WHERE user_id = :userId AND created_at BETWEEN :startTime AND :endTime")
    Mono<java.math.BigDecimal> sumTotalCostByUserIdAndCreatedAtBetween(Long userId, OffsetDateTime startTime, OffsetDateTime endTime);

    Flux<RequestLog> findTop100ByOrderByCreatedAtDesc();
}
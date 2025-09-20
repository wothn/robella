package org.elmo.robella.repository;

import org.elmo.robella.model.entity.ApiKey;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


@Repository
public interface ApiKeyRepository extends R2dbcRepository<ApiKey, Long> {

    Flux<ApiKey> findByUserId(Long userId);

    Mono<ApiKey> findByKeyHash(String keyHash);

    Flux<ApiKey> findByUserIdAndActive(Long userId, Boolean active);

    Mono<ApiKey> findByKeyPrefix(String keyPrefix);

    Mono<Void> deleteByIdAndUserId(Long id, Long userId);
}
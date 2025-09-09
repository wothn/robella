package org.elmo.robella.repository;

import org.elmo.robella.model.entity.Provider;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ProviderRepository extends R2dbcRepository<Provider, Long> {
    Flux<Provider> findByEnabledTrue();
    Mono<Provider> findByName(String name);
    Mono<Provider> findByEnabledTrueAndName(String name);
    Flux<Provider> findByType(String type);
}
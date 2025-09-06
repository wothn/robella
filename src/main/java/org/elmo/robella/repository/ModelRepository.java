package org.elmo.robella.repository;

import java.util.Collection;
import org.elmo.robella.model.Model;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ModelRepository extends R2dbcRepository<Model, Long> {
    Flux<Model> findByProviderId(Long providerId);
    Flux<Model> findByProviderIdAndActiveTrue(Long providerId);
    Flux<Model> findByActiveTrue();
    Mono<Model> findByProviderIdAndName(Long providerId, String name);
    Flux<Model> findByProviderIdInAndActiveTrue(Collection<Long> providerIds);
}
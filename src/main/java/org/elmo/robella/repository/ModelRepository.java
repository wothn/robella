package org.elmo.robella.repository;

import java.util.Collection;
import org.elmo.robella.model.Model;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ModelRepository extends R2dbcRepository<Model, Long> {
    Flux<Model> findByProviderId(Long providerId);
    Flux<Model> findByProviderIdAndEnabledTrue(Long providerId);
    Flux<Model> findByEnabledTrue();
    Mono<Model> findByProviderIdAndName(Long providerId, String name);
    Flux<Model> findByProviderIdInAndEnabledTrue(Collection<Long> providerIds);
    
    // New query methods for enhanced model features
    Flux<Model> findByGroupAndEnabledTrue(String group);
    Flux<Model> findBySupportedTextDeltaTrueAndEnabledTrue();
    Flux<Model> findByOwnedByAndEnabledTrue(String ownedBy);
    Flux<Model> findByGroupAndSupportedTextDeltaTrueAndEnabledTrue(String group);
    Flux<Model> findByCapabilitiesContainingAndEnabledTrue(String capability);
    Flux<Model> findByGroupAndProviderIdAndEnabledTrue(String group, Long providerId);
}
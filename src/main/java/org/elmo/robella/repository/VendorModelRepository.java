package org.elmo.robella.repository;

import org.elmo.robella.model.entity.VendorModel;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface VendorModelRepository extends R2dbcRepository<VendorModel, Long> {
    Flux<VendorModel> findByModelId(Long modelId);
    Flux<VendorModel> findByProviderId(Long providerId);
    Flux<VendorModel> findByModelIdAndProviderId(Long modelId, Long providerId);
    Flux<VendorModel> findByModelIdAndProviderIdAndEnabledTrue(Long modelId, Long providerId);
    Flux<VendorModel> findByEnabledTrue();
    Mono<VendorModel> findByModelIdAndProviderIdAndVendorModelName(Long modelId, Long providerId, String vendorModelName);
    Mono<VendorModel> findByVendorModelName(String vendorModelName);
}
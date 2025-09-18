package org.elmo.robella.controller;

import org.elmo.robella.annotation.RequiredRole;
import org.elmo.robella.model.common.Role;
import org.elmo.robella.model.entity.Provider;
import org.elmo.robella.model.entity.VendorModel;
import org.elmo.robella.service.ProviderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/providers")
public class ProviderController {

    @Autowired
    private ProviderService providerService;

    @GetMapping
    public Flux<Provider> getAllProviders() {
        return providerService.getAllProviders();
    }

    @GetMapping("/active")
    @RequiredRole(Role.ROOT)
    public Flux<Provider> getActiveProviders() {
        return providerService.getEnabledProviders();
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<Provider>> getProviderById(@PathVariable Long id) {
        return providerService.getProviderById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PostMapping
    @RequiredRole(Role.ROOT)
    public Mono<ResponseEntity<Provider>> createProvider(@RequestBody Provider provider) {
        return providerService.createProvider(provider)
                .map(savedProvider -> ResponseEntity.ok(savedProvider));
    }

    @PutMapping("/{id}")
    @RequiredRole(Role.ROOT)
    public Mono<ResponseEntity<Provider>> updateProvider(@PathVariable Long id, @RequestBody Provider provider) {
        return providerService.updateProvider(id, provider)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @RequiredRole(Role.ROOT)
    public Mono<ResponseEntity<Void>> deleteProvider(@PathVariable Long id) {
        return providerService.deleteProvider(id)
                .then(Mono.just(ResponseEntity.ok().<Void>build()))
                .onErrorResume(e -> Mono.just(ResponseEntity.badRequest().build()));
    }

    @GetMapping("/{id}/models")
    public Flux<VendorModel> getVendorModelsByProviderId(@PathVariable Long id) {
        return providerService.getVendorModelsByProviderId(id);
    }

    @PostMapping("/{id}/models")
    @RequiredRole(Role.ROOT)
    public Mono<ResponseEntity<VendorModel>> createModel(@PathVariable Long id, @RequestBody VendorModel model) {
        return providerService.createVendorModel(id, model)
                .map(savedModel -> ResponseEntity.ok(savedModel));
    }

    @PutMapping("/models/{id}")
    @RequiredRole(Role.ROOT)
    public Mono<ResponseEntity<VendorModel>> updateModel(@PathVariable Long id, @RequestBody VendorModel model) {
        return providerService.updateVendorModel(id, model)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/models/{id}")
    @RequiredRole(Role.ROOT)
    public Mono<ResponseEntity<Void>> deleteVendorModel(@PathVariable Long id) {
        return providerService.deleteVendorModel(id)
                .then(Mono.just(ResponseEntity.ok().<Void>build()));
    }

}
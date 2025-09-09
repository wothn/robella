package org.elmo.robella.controller;


import org.elmo.robella.model.entity.Model;
import org.elmo.robella.model.entity.Provider;
import org.elmo.robella.service.ProviderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/providers")
@CrossOrigin(origins = "*")
public class ProviderController {
    
    @Autowired
    private ProviderService providerService;
    
    @GetMapping
    public Flux<Provider> getAllProviders() {
        return providerService.getAllProviders();
    }
    
    @GetMapping("/active")
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
        public Mono<ResponseEntity<Provider>> createProvider(@RequestBody Provider provider) {
        return providerService.createProvider(provider)
                .map(savedProvider -> ResponseEntity.ok(savedProvider));
    }
    
    @PutMapping("/{id}")
        public Mono<ResponseEntity<Provider>> updateProvider(@PathVariable Long id, @RequestBody Provider provider) {
        return providerService.updateProvider(id, provider)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
    
    @PatchMapping("/{id}")
        public Mono<ResponseEntity<Provider>> patchProvider(@PathVariable Long id, @RequestBody Provider provider) {
        return providerService.patchProvider(id, provider)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
    
    @DeleteMapping("/{id}")
        public Mono<ResponseEntity<Void>> deleteProvider(@PathVariable Long id) {
        return providerService.deleteProvider(id)
                .then(Mono.just(ResponseEntity.ok().<Void>build()))
                .onErrorResume(e -> Mono.just(ResponseEntity.badRequest().build()));
    }
    
    @GetMapping("/{id}/models")
        public Flux<Model> getModelsByProviderId(@PathVariable Long id) {
        return providerService.getModelsByProviderId(id);
    }
    
    @GetMapping("/{id}/models/active")
    public Flux<Model> getActiveModelsByProviderId(@PathVariable Long id) {
        return providerService.getActiveModelsByProviderId(id);
    }
    
    @PostMapping("/{id}/models")
        public Mono<ResponseEntity<Model>> createModel(@PathVariable Long id, @RequestBody Model model) {
        return providerService.createModel(model)
                .map(savedModel -> ResponseEntity.ok(savedModel));
    }
    
    @PutMapping("/models/{modelId}")
        public Mono<ResponseEntity<Model>> updateModel(@PathVariable Long modelId, @RequestBody Model model) {
        return providerService.updateModel(modelId, model)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
    
    @PatchMapping("/models/{modelId}")
        public Mono<ResponseEntity<Model>> patchModel(@PathVariable Long modelId, @RequestBody Model model) {
        return providerService.patchModel(modelId, model)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
    
    @DeleteMapping("/models/{modelId}")
        public Mono<ResponseEntity<Void>> deleteModel(@PathVariable Long modelId) {
        return providerService.deleteModel(modelId)
                .then(Mono.just(ResponseEntity.ok().<Void>build()));
    }
    
    @GetMapping("/models/active")
    public Flux<Model> getAllActiveModels() {
        return providerService.getAllActiveModels();
    }
}
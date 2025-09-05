package org.elmo.robella.service;

import org.elmo.robella.model.Provider;
import org.elmo.robella.model.Model;
import org.elmo.robella.repository.ProviderRepository;
import org.elmo.robella.repository.ModelRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.time.LocalDateTime;

@Service
public class ProviderService {
    
    @Autowired
    private ProviderRepository providerRepository;
    
    @Autowired
    private ModelRepository modelRepository;
    
    public Flux<Provider> getAllProviders() {
        return providerRepository.findAll();
    }
    
    public Flux<Provider> getActiveProviders() {
        return providerRepository.findByActiveTrue();
    }
    
    public Mono<Provider> getProviderById(Long id) {
        return providerRepository.findById(id);
    }
    
    public Mono<Provider> getProviderByName(String name) {
        return providerRepository.findByName(name);
    }
    
    public Mono<Provider> createProvider(Provider provider) {
        provider.setCreatedAt(LocalDateTime.now());
        provider.setUpdatedAt(LocalDateTime.now());
        provider.setActive(true);
        return providerRepository.save(provider);
    }
    
    public Mono<Provider> updateProvider(Long id, Provider provider) {
        return providerRepository.findById(id)
                .flatMap(existingProvider -> {
                    existingProvider.setName(provider.getName());
                    existingProvider.setType(provider.getType());
                    existingProvider.setApiKey(provider.getApiKey());
                    existingProvider.setBaseUrl(provider.getBaseUrl());
                    existingProvider.setDeploymentName(provider.getDeploymentName());
                    existingProvider.setActive(provider.getActive());
                    existingProvider.setUpdatedAt(LocalDateTime.now());
                    return providerRepository.save(existingProvider);
                });
    }
    
    public Mono<Provider> patchProvider(Long id, Provider provider) {
        return providerRepository.findById(id)
                .flatMap(existingProvider -> {
                    if (provider.getName() != null) existingProvider.setName(provider.getName());
                    if (provider.getType() != null) existingProvider.setType(provider.getType());
                    if (provider.getApiKey() != null) existingProvider.setApiKey(provider.getApiKey());
                    if (provider.getBaseUrl() != null) existingProvider.setBaseUrl(provider.getBaseUrl());
                    if (provider.getDeploymentName() != null) existingProvider.setDeploymentName(provider.getDeploymentName());
                    if (provider.getActive() != null) existingProvider.setActive(provider.getActive());
                    existingProvider.setUpdatedAt(LocalDateTime.now());
                    return providerRepository.save(existingProvider);
                });
    }
    
    public Mono<Void> deleteProvider(Long id) {
        return modelRepository.findByProviderId(id)
                .collectList()
                .flatMap(models -> {
                    if (!models.isEmpty()) {
                        return Mono.error(new RuntimeException("Cannot delete provider with existing models"));
                    }
                    return providerRepository.deleteById(id);
                });
    }
    
    public Flux<Model> getModelsByProviderId(Long providerId) {
        return modelRepository.findByProviderId(providerId);
    }
    
    public Flux<Model> getActiveModelsByProviderId(Long providerId) {
        return modelRepository.findByProviderIdAndActiveTrue(providerId);
    }
    
    public Mono<Model> getModelById(Long id) {
        return modelRepository.findById(id);
    }
    
    public Mono<Model> createModel(Model model) {
        model.setCreatedAt(LocalDateTime.now());
        model.setUpdatedAt(LocalDateTime.now());
        model.setActive(true);
        return modelRepository.save(model);
    }
    
    public Mono<Model> updateModel(Long id, Model model) {
        return modelRepository.findById(id)
                .flatMap(existingModel -> {
                    existingModel.setProviderId(model.getProviderId());
                    existingModel.setName(model.getName());
                    existingModel.setVendorModel(model.getVendorModel());
                    existingModel.setActive(model.getActive());
                    existingModel.setUpdatedAt(LocalDateTime.now());
                    return modelRepository.save(existingModel);
                });
    }
    
    public Mono<Model> patchModel(Long id, Model model) {
        return modelRepository.findById(id)
                .flatMap(existingModel -> {
                    if (model.getProviderId() != null) existingModel.setProviderId(model.getProviderId());
                    if (model.getName() != null) existingModel.setName(model.getName());
                    if (model.getVendorModel() != null) existingModel.setVendorModel(model.getVendorModel());
                    if (model.getActive() != null) existingModel.setActive(model.getActive());
                    existingModel.setUpdatedAt(LocalDateTime.now());
                    return modelRepository.save(existingModel);
                });
    }
    
    public Mono<Void> deleteModel(Long id) {
        return modelRepository.deleteById(id);
    }
    
    public Flux<Model> getAllActiveModels() {
        return modelRepository.findByActiveTrue();
    }
    
    public Mono<Provider> getActiveProviderByName(String name) {
        return providerRepository.findByActiveTrueAndName(name);
    }
}
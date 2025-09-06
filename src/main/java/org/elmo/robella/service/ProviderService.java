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
        return providerRepository.findByEnabledTrue();
    }
    
    public Flux<Provider> getEnabledProviders() {
        return providerRepository.findByEnabledTrue();
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
        provider.setEnabled(true);
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
                    existingProvider.setEnabled(provider.getEnabled());
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
                    if (provider.getEnabled() != null) existingProvider.setEnabled(provider.getEnabled());
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
        return modelRepository.findByProviderIdAndEnabledTrue(providerId);
    }
    
    public Mono<Model> getModelById(Long id) {
        return modelRepository.findById(id);
    }
    
    public Mono<Model> createModel(Model model) {
        model.setCreatedAt(LocalDateTime.now());
        model.setUpdatedAt(LocalDateTime.now());
        model.setEnabled(true);
        return modelRepository.save(model);
    }
    
    public Mono<Model> updateModel(Long id, Model model) {
        return modelRepository.findById(id)
                .flatMap(existingModel -> {
                    existingModel.setProviderId(model.getProviderId());
                    existingModel.setName(model.getName());
                    existingModel.setGroup(model.getGroup());
                    existingModel.setOwnedBy(model.getOwnedBy());
                    existingModel.setDescription(model.getDescription());
                    existingModel.setCapabilities(model.getCapabilities());
                    existingModel.setInputPerMillionTokens(model.getInputPerMillionTokens());
                    existingModel.setOutputPerMillionTokens(model.getOutputPerMillionTokens());
                    existingModel.setCurrencySymbol(model.getCurrencySymbol());
                    existingModel.setCachedInputPrice(model.getCachedInputPrice());
                    existingModel.setCachedOutputPrice(model.getCachedOutputPrice());
                    existingModel.setSupportedTextDelta(model.getSupportedTextDelta());
                    existingModel.setVendorModel(model.getVendorModel());
                    existingModel.setEnabled(model.getEnabled());
                    existingModel.setUpdatedAt(LocalDateTime.now());
                    return modelRepository.save(existingModel);
                });
    }
    
    public Mono<Model> patchModel(Long id, Model model) {
        return modelRepository.findById(id)
                .flatMap(existingModel -> {
                    if (model.getProviderId() != null) existingModel.setProviderId(model.getProviderId());
                    if (model.getName() != null) existingModel.setName(model.getName());
                    if (model.getGroup() != null) existingModel.setGroup(model.getGroup());
                    if (model.getOwnedBy() != null) existingModel.setOwnedBy(model.getOwnedBy());
                    if (model.getDescription() != null) existingModel.setDescription(model.getDescription());
                    if (model.getCapabilities() != null) existingModel.setCapabilities(model.getCapabilities());
                    if (model.getInputPerMillionTokens() != null) existingModel.setInputPerMillionTokens(model.getInputPerMillionTokens());
                    if (model.getOutputPerMillionTokens() != null) existingModel.setOutputPerMillionTokens(model.getOutputPerMillionTokens());
                    if (model.getCurrencySymbol() != null) existingModel.setCurrencySymbol(model.getCurrencySymbol());
                    if (model.getCachedInputPrice() != null) existingModel.setCachedInputPrice(model.getCachedInputPrice());
                    if (model.getCachedOutputPrice() != null) existingModel.setCachedOutputPrice(model.getCachedOutputPrice());
                    if (model.getSupportedTextDelta() != null) existingModel.setSupportedTextDelta(model.getSupportedTextDelta());
                    if (model.getVendorModel() != null) existingModel.setVendorModel(model.getVendorModel());
                    if (model.getEnabled() != null) existingModel.setEnabled(model.getEnabled());
                    existingModel.setUpdatedAt(LocalDateTime.now());
                    return modelRepository.save(existingModel);
                });
    }
    
    public Mono<Void> deleteModel(Long id) {
        return modelRepository.deleteById(id);
    }
    
    public Flux<Model> getAllActiveModels() {
        return modelRepository.findByEnabledTrue();
    }
    
    // New methods for enhanced model features
    public Flux<Model> getActiveModelsByGroup(String group) {
        return modelRepository.findByGroupAndEnabledTrue(group);
    }
    
    public Flux<Model> getActiveModelsWithTextDelta() {
        return modelRepository.findBySupportedTextDeltaTrueAndEnabledTrue();
    }
    
    public Flux<Model> getActiveModelsByOwner(String ownedBy) {
        return modelRepository.findByOwnedByAndEnabledTrue(ownedBy);
    }
    
    public Flux<Model> getActiveModelsByGroupAndTextDelta(String group) {
        return modelRepository.findByGroupAndSupportedTextDeltaTrueAndEnabledTrue(group);
    }
    
    public Flux<Model> getActiveModelsByCapability(String capability) {
        return modelRepository.findByCapabilitiesContainingAndEnabledTrue(capability);
    }
    
    public Flux<Model> getActiveModelsByGroupAndProvider(String group, Long providerId) {
        return modelRepository.findByGroupAndProviderIdAndEnabledTrue(group, providerId);
    }
    
    public Mono<Provider> getActiveProviderByName(String name) {
        return providerRepository.findByEnabledTrueAndName(name);
    }
}
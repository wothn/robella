package org.elmo.robella.service;

import org.elmo.robella.model.entity.Model;
import org.elmo.robella.model.entity.Provider;
import org.elmo.robella.model.entity.VendorModel;
import org.elmo.robella.repository.ProviderRepository;
import org.elmo.robella.repository.ModelRepository;
import org.elmo.robella.repository.VendorModelRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class ProviderService {
    
    @Autowired
    private ProviderRepository providerRepository;
    
    @Autowired
    private ModelRepository modelRepository;
    
    @Autowired
    private VendorModelRepository vendorModelRepository;
    
    // Provider methods
    public Flux<Provider> getAllProviders() {
        return providerRepository.findAll();
    }
    
    public Flux<Provider> getEnabledProviders() {
        return providerRepository.findByEnabledTrue();
    }
    
    public Mono<Provider> getProviderById(Long id) {
        return providerRepository.findById(id);
    }
    
    public Mono<Provider> findById(Long id) {
        return providerRepository.findById(id);
    }
    
    public Mono<Provider> getProviderByName(String name) {
        return providerRepository.findByName(name);
    }
    
    public Mono<Provider> createProvider(Provider provider) {
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
                    existingProvider.setConfig(provider.getConfig());
                    existingProvider.setEnabled(provider.getEnabled());
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
                    if (provider.getConfig() != null) existingProvider.setConfig(provider.getConfig());
                    if (provider.getEnabled() != null) existingProvider.setEnabled(provider.getEnabled());
                    return providerRepository.save(existingProvider);
                });
    }
    
    public Mono<Void> deleteProvider(Long id) {
        return vendorModelRepository.findByProviderId(id)
                .collectList()
                .flatMap(vendorModels -> {
                    if (!vendorModels.isEmpty()) {
                        return Mono.error(new RuntimeException("Cannot delete provider with existing vendor models"));
                    }
                    return providerRepository.deleteById(id);
                });
    }
    
    // Model methods
    public Flux<Model> getAllModels() {
        return modelRepository.findAll();
    }
    
    public Flux<Model> getPublishedModels() {
        return modelRepository.findByIsPublishedTrue();
    }
    
    public Mono<Model> getModelById(Long id) {
        return modelRepository.findById(id);
    }
    
    public Mono<Model> getModelByName(String name) {
        return modelRepository.findByName(name);
    }
    
    public Mono<Model> createModel(Model model) {
        model.setIsPublished(true);
        return modelRepository.save(model);
    }
    
    public Mono<Model> updateModel(Long id, Model model) {
        return modelRepository.findById(id)
                .flatMap(existingModel -> {
                    existingModel.setName(model.getName());
                    existingModel.setDescription(model.getDescription());
                    existingModel.setOrganization(model.getOrganization());
                    existingModel.setCapabilities(model.getCapabilities());
                    existingModel.setContextWindow(model.getContextWindow());
                    existingModel.setIsPublished(model.getIsPublished());
                    return modelRepository.save(existingModel);
                });
    }
    
    public Mono<Model> patchModel(Long id, Model model) {
        return modelRepository.findById(id)
                .flatMap(existingModel -> {
                    if (model.getName() != null) existingModel.setName(model.getName());
                    if (model.getDescription() != null) existingModel.setDescription(model.getDescription());
                    if (model.getOrganization() != null) existingModel.setOrganization(model.getOrganization());
                    if (model.getCapabilities() != null) existingModel.setCapabilities(model.getCapabilities());
                    if (model.getContextWindow() != null) existingModel.setContextWindow(model.getContextWindow());
                    if (model.getIsPublished() != null) existingModel.setIsPublished(model.getIsPublished());
                    return modelRepository.save(existingModel);
                });
    }
    
    public Mono<Void> deleteModel(Long id) {
        return vendorModelRepository.findByModelId(id)
                .collectList()
                .flatMap(vendorModels -> {
                    if (!vendorModels.isEmpty()) {
                        return Mono.error(new RuntimeException("Cannot delete model with existing vendor models"));
                    }
                    return modelRepository.deleteById(id);
                });
    }
    
    // VendorModel methods
    public Flux<VendorModel> getVendorModelsByModelId(Long modelId) {
        return vendorModelRepository.findByModelId(modelId);
    }
    
    public Flux<VendorModel> getVendorModelsByProviderId(Long providerId) {
        return vendorModelRepository.findByProviderId(providerId);
    }
    
    public Flux<VendorModel> getEnabledVendorModels(Long modelId, Long providerId) {
        return vendorModelRepository.findByModelIdAndProviderIdAndEnabledTrue(modelId, providerId);
    }
    
    public Mono<VendorModel> getVendorModel(Long modelId, Long providerId, String vendorModelName) {
        return vendorModelRepository.findByModelIdAndProviderIdAndVendorModelName(modelId, providerId, vendorModelName);
    }
    
    public Mono<VendorModel> createVendorModel(VendorModel vendorModel) {
        vendorModel.setEnabled(true);
        return vendorModelRepository.save(vendorModel);
    }
    
    public Mono<VendorModel> updateVendorModel(Long id, VendorModel vendorModel) {
        return vendorModelRepository.findById(id)
                .flatMap(existingVendorModel -> {
                    existingVendorModel.setModelId(vendorModel.getModelId());
                    existingVendorModel.setProviderId(vendorModel.getProviderId());
                    existingVendorModel.setVendorModelName(vendorModel.getVendorModelName());
                    existingVendorModel.setDescription(vendorModel.getDescription());
                    existingVendorModel.setPricing(vendorModel.getPricing());
                    existingVendorModel.setEnabled(vendorModel.getEnabled());
                    return vendorModelRepository.save(existingVendorModel);
                });
    }
    
    public Mono<VendorModel> patchVendorModel(Long id, VendorModel vendorModel) {
        return vendorModelRepository.findById(id)
                .flatMap(existingVendorModel -> {
                    if (vendorModel.getModelId() != null) existingVendorModel.setModelId(vendorModel.getModelId());
                    if (vendorModel.getProviderId() != null) existingVendorModel.setProviderId(vendorModel.getProviderId());
                    if (vendorModel.getVendorModelName() != null) existingVendorModel.setVendorModelName(vendorModel.getVendorModelName());
                    if (vendorModel.getDescription() != null) existingVendorModel.setDescription(vendorModel.getDescription());
                    if (vendorModel.getPricing() != null) existingVendorModel.setPricing(vendorModel.getPricing());
                    if (vendorModel.getEnabled() != null) existingVendorModel.setEnabled(vendorModel.getEnabled());
                    return vendorModelRepository.save(existingVendorModel);
                });
    }
    
    public Mono<Void> deleteVendorModel(Long id) {
        return vendorModelRepository.deleteById(id);
    }
    
    // Helper methods for routing
    public Flux<Model> getAllActiveModels() {
        return getPublishedModels();
    }
    
    // Additional helper methods for controller
    public Flux<Model> getModelsByProviderId(Long providerId) {
        return vendorModelRepository.findByProviderId(providerId)
                .flatMap(vendorModel -> modelRepository.findById(vendorModel.getModelId()));
    }
    
    public Flux<Model> getActiveModelsByProviderId(Long providerId) {
        return vendorModelRepository.findByProviderId(providerId)
                .filter(VendorModel::getEnabled)
                .flatMap(vendorModel -> modelRepository.findById(vendorModel.getModelId()))
                .filter(Model::getIsPublished);
    }
    
    public Mono<Provider> getActiveProviderByName(String name) {
        return providerRepository.findByEnabledTrueAndName(name);
    }
}
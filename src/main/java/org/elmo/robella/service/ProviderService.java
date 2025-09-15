package org.elmo.robella.service;

import org.elmo.robella.model.entity.Provider;
import org.elmo.robella.model.entity.VendorModel;
import org.elmo.robella.repository.ProviderRepository;
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
                    existingProvider.setEndpointType(provider.getEndpointType());
                    existingProvider.setProviderType(provider.getProviderType());
                    existingProvider.setApiKey(provider.getApiKey());
                    existingProvider.setBaseUrl(provider.getBaseUrl());
                    existingProvider.setConfig(provider.getConfig());
                    existingProvider.setEnabled(provider.getEnabled());
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
    
      
    // VendorModel methods
    public Flux<VendorModel> getVendorModelsByProviderId(Long providerId) {
        return vendorModelRepository.findByProviderId(providerId);
    }
    
    public Mono<VendorModel> createVendorModel(Long providerId, VendorModel vendorModel) {
        vendorModel.setProviderId(providerId);
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
                    existingVendorModel.setInputPerMillionTokens(vendorModel.getInputPerMillionTokens());
                    existingVendorModel.setOutputPerMillionTokens(vendorModel.getOutputPerMillionTokens());
                    existingVendorModel.setCurrency(vendorModel.getCurrency());
                    existingVendorModel.setCachedInputPrice(vendorModel.getCachedInputPrice());
                    existingVendorModel.setCachedOutputPrice(vendorModel.getCachedOutputPrice());
                    existingVendorModel.setEnabled(vendorModel.getEnabled());
                    return vendorModelRepository.save(existingVendorModel);
                });
    }
    
    public Mono<Void> deleteVendorModel(Long id) {
        return vendorModelRepository.deleteById(id);
    }
}
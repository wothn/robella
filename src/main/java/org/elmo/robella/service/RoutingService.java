package org.elmo.robella.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import org.elmo.robella.client.ClientFactory;
import org.elmo.robella.client.ApiClient;
import org.elmo.robella.model.entity.VendorModel;
import org.elmo.robella.repository.ModelRepository;
import org.elmo.robella.repository.VendorModelRepository;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class RoutingService {

    private final ClientFactory clientFactory;
    private final ProviderService providerService;
    private final VendorModelRepository vendorModelRepository;
    private final ModelRepository modelRepository;

    public Mono<VendorModel> selectVendor(String modelName) {
        return modelRepository.findByName(modelName)
            .flatMap(model -> vendorModelRepository.findByModelId(model.getId())
                .filter(VendorModel::getEnabled)
                .next()
            );
    }

    /**
     * Get client by model name
     */
    public Mono<ApiClient> getClientByModel(String modelName) {
        return selectVendor(modelName)
            .flatMap(vendorModel -> providerService.findById(vendorModel.getProviderId())
                .map(provider -> clientFactory.getClient(provider.getName())));
    }

    /**
     * Get provider type by provider name
     */
    public String getProviderType(String providerName) {
        return providerService.getProviderByName(providerName)
            .map(provider -> provider.getType())
            .block();
    }




}
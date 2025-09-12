package org.elmo.robella.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import org.elmo.robella.client.ClientFactory;
import org.elmo.robella.client.ApiClient;
import org.elmo.robella.model.entity.VendorModel;
import org.elmo.robella.model.common.EndpointType;
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
     * 将客户端模型名称映射到供应商模型名称
     * 
     * @param clientModelName 客户端请求中的模型名称
     * @return 供应商特定的模型名称，如果找不到映射则返回原名称
     */
    public Mono<String> mapToVendorModelName(String clientModelName) {
        return selectVendor(clientModelName)
                .map(VendorModel::getVendorModelName)
                .doOnNext(vendorModelName -> log.debug("Mapped client model '{}' to vendor model '{}'", 
                        clientModelName, vendorModelName))
                .doOnError(error -> log.warn("Failed to map model '{}': {}", clientModelName, error.getMessage()))
                .onErrorReturn(clientModelName) // 如果映射失败，返回原模型名称
                .switchIfEmpty(Mono.fromSupplier(() -> {
                    log.warn("No vendor model mapping found for client model '{}', using original name", clientModelName);
                    return clientModelName;
                }));
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
    public EndpointType getProviderType(String providerName) {
        return providerService.getProviderByName(providerName)
            .map(provider -> provider.getType())
            .block();
    }

    /**
     * Get provider type by model name
     */
    public Mono<EndpointType> getProviderTypeByModel(String modelName) {
        return selectVendor(modelName)
            .flatMap(vendorModel -> providerService.findById(vendorModel.getProviderId())
                .map(provider -> provider.getType()));
    }

}
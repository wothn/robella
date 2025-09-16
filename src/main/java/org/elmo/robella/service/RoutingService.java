package org.elmo.robella.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import org.elmo.robella.client.ClientFactory;
import org.elmo.robella.common.EndpointType;
import org.elmo.robella.client.ApiClient;
import org.elmo.robella.model.entity.Provider;
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

    /**
     * 根据模型名称选择一个启用的供应商模型。
     *
     * @param modelName 客户端请求的模型名称
     * @return Mono<VendorModel> 启用的供应商模型，如果未找到则为空
     */
    public Mono<VendorModel> selectVendor(String modelName) {
        return modelRepository.findByName(modelName)
            .flatMap(model -> vendorModelRepository.findByModelId(model.getId())
                .filter(VendorModel::getEnabled)
                .next()
            );
    }

    /**
     * 将客户端模型名称映射到供应商模型调用标识
     *
     * @param clientModelName 客户端请求中的模型名称
     * @return 供应商特定的模型调用标识，如果找不到映射则返回原名称
     */
    public Mono<String> mapToVendorModelKey(String clientModelName) {
        return selectVendor(clientModelName)
                .map(VendorModel::getModelKey)
                .doOnNext(modelKey -> log.debug("Mapped client model '{}' to vendor model key '{}'",
                        clientModelName, modelKey))
                .doOnError(error -> log.warn("Failed to map model '{}': {}", clientModelName, error.getMessage()))
                .onErrorReturn(clientModelName) // 如果映射失败，返回原模型名称
                .switchIfEmpty(Mono.fromSupplier(() -> {
                    log.warn("No vendor model mapping found for client model '{}', using original name", clientModelName);
                    return clientModelName;
                }));
    }


    /**
     * 根据供应商模型调用标识获取对应的 API 客户端和 Provider。
     * <p>
     * 该方法通过供应商模型调用标识查找启用的 VendorModel，
     * 然后根据其 providerId 获取 Provider，并通过 ClientFactory 获取对应的 ApiClient 实例。
     *
     * @param modelKey 供应商模型调用标识
     * @return Mono<ClientWithProvider> 对应的 API 客户端、Provider 和 VendorModel，如果未找到则为空
     */
    public Mono<ClientWithProvider> getClientWithProviderByModelKey(String modelKey) {
        return vendorModelRepository.findByModelKey(modelKey)
            .filter(VendorModel::getEnabled)
            .flatMap(vendorModel -> providerService.findById(vendorModel.getProviderId())
                .map(provider -> {
                    ApiClient client = clientFactory.getClient(provider.getEndpointType());
                    return new ClientWithProvider(client, provider, vendorModel);
                }));
    }

    /**
     * Get provider type by provider name
     */
    /**
     * 根据供应商名称获取其 EndpointType 类型。
     *
     * @param providerName 供应商名称
     * @return EndpointType 供应商类型，如果未找到则为 null
     */
    public EndpointType getProviderType(String providerName) {
        return providerService.getProviderByName(providerName)
            .map(provider -> provider.getEndpointType())
            .block();
    }


    /**
     * 通过供应商模型调用标识直接获取 EndpointType 类型。
     *
     * @param modelKey 供应商模型调用标识
     * @return Mono<EndpointType> 供应商类型，如果未找到则为空
     */
    public Mono<EndpointType> getProviderTypeByModelKey(String modelKey) {
        return vendorModelRepository.findByModelKey(modelKey)
            .filter(VendorModel::getEnabled)
            .flatMap(vendorModel -> providerService.findById(vendorModel.getProviderId())
                .map(provider -> provider.getEndpointType()));
    }

    /**
     * 客户端、Provider 和 VendorModel 的封装类
     */
    public static class ClientWithProvider {
        private final ApiClient client;
        private final Provider provider;
        private final VendorModel vendorModel;
        
        public ClientWithProvider(ApiClient client, Provider provider, VendorModel vendorModel) {
            this.client = client;
            this.provider = provider;
            this.vendorModel = vendorModel;
        }
        
        public ApiClient getClient() {
            return client;
        }
        
        public Provider getProvider() {
            return provider;
        }
        
        public VendorModel getVendorModel() {
            return vendorModel;
        }
    }

}
package org.elmo.robella.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import org.elmo.robella.client.ClientFactory;

import java.util.List;

import org.elmo.robella.client.ApiClient;
import org.elmo.robella.model.entity.Provider;
import org.elmo.robella.model.entity.VendorModel;
import org.elmo.robella.repository.ModelRepository;
import org.elmo.robella.repository.VendorModelRepository;
import org.elmo.robella.service.loadblancer.LoadBalancerStrategy;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class RoutingService {

    private final ClientFactory clientFactory;
    private final ProviderService providerService;
    private final VendorModelRepository vendorModelRepository;
    private final ModelRepository modelRepository;
    private final LoadBalancerStrategy loadBalancer;

    /**     
     * 获取所有启用供应商模型（用于负载均衡）     
     */    
    public Mono<List<VendorModel>> selectAllEnabledVendors(String modelKey) {
        return modelRepository.findByModelKey(modelKey)
            .flatMapMany(model -> vendorModelRepository.findByModelIdAndEnabledTrue(model.getId()))
            .collectList();
    }

    /**
     * 使用负载均衡策略选择一个启用的供应商模型
     *
     * @param modelKey 客户端请求中的模型调用标识
     * @return 选定的供应商模型，如果没有可用供应商则返回空
     */
    public Mono<VendorModel> selectVendorWithLoadBalancing(String modelKey) {
        return selectAllEnabledVendors(modelKey)
            .filter(candidates -> !candidates.isEmpty())
            .map(loadBalancer::select)
            .switchIfEmpty(Mono.empty());
    }

    /**
     * 将客户端模型调用标识映射到供应商模型调用标识
     *
     * @param clientModelKey 客户端请求中的模型调用标识
     * @return 供应商特定的模型调用标识，如果找不到映射则返回原标识
     */
    public Mono<String> mapToVendorModelKey(String clientModelKey) {
        return selectVendorWithLoadBalancing(clientModelKey)
                .map(VendorModel::getVendorModelKey)
                .doOnNext(vendorModelKey -> log.debug("Mapped client model key '{}' to vendor model key '{}'",
                        clientModelKey, vendorModelKey))
                .doOnError(error -> log.warn("Failed to map model '{}': {}", clientModelKey, error.getMessage()))
                .onErrorReturn(clientModelKey) // 如果映射失败，返回原模型标识
                .switchIfEmpty(Mono.fromSupplier(() -> {
                    log.warn("No vendor model mapping found for client model '{}', using original key", clientModelKey);
                    return clientModelKey;
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
    public Mono<ClientWithProvider> getClientWithProviderByModelKey(String vendorModelKey) {
        return vendorModelRepository.findByVendorModelKeyAndEnabledTrue(vendorModelKey)
            .flatMap(vendorModel -> providerService.findById(vendorModel.getProviderId())
                .map(provider -> {
                    ApiClient client = clientFactory.getClient(provider.getEndpointType());
                    return new ClientWithProvider(client, provider, vendorModel);
                }));
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
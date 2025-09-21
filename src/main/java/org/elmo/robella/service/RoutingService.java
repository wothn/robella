package org.elmo.robella.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.elmo.robella.client.ClientFactory;

import java.util.List;

import org.elmo.robella.client.ApiClient;
import org.elmo.robella.model.entity.Provider;
import org.elmo.robella.model.entity.VendorModel;
import org.elmo.robella.mapper.VendorModelMapper;
import org.elmo.robella.service.loadblancer.LoadBalancerStrategy;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class RoutingService {

    private final ClientFactory clientFactory;
    private final ProviderService providerService;
    private final VendorModelMapper vendorModelMapper;
    private final LoadBalancerStrategy loadBalancer;

    /**
     * 使用负载均衡策略选择一个启用的供应商模型
     *
     * @param modelKey 客户端请求中的模型调用标识
     * @return 选定的供应商模型，如果没有可用供应商则返回空
     */
    public VendorModel selectVendor(String modelKey) {
        List<VendorModel> candidates = vendorModelMapper.findByModelKeyAndEnabledTrue(modelKey);
        if (candidates.isEmpty()) {
            return null;
        }
        return loadBalancer.select(candidates);
    }

    /**
     * 根据供应商模型调用标识获取对应的 API 客户端和 Provider。
     * <p>
     * 该方法通过供应商模型调用标识查找启用的 VendorModel，
     * 然后根据其 providerId 获取 Provider，并通过 ClientFactory 获取对应的 ApiClient 实例。
     *
     * @param vendorModelKey 供应商模型调用标识
     * @return ClientWithInfo 对应的 API 客户端、Provider 和 VendorModel，如果未找到则为空
     */
    public ClientWithInfo routeAndClient(String modelKey) {
        VendorModel vendorModel = selectVendor(modelKey);
        if (vendorModel == null) {
            return null;
        }

        Provider provider = providerService.getProviderById(vendorModel.getProviderId());
        if (provider == null) {
            return null;
        }

        ApiClient client = clientFactory.getClient(provider.getEndpointType());
        return new ClientWithInfo(client, provider, vendorModel);
    }

    /**
     * 客户端、Provider 和 VendorModel 的封装类
     */
    public static class ClientWithInfo {
        private final ApiClient client;
        private final Provider provider;
        private final VendorModel vendorModel;

        public ClientWithInfo(ApiClient client, Provider provider, VendorModel vendorModel) {
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
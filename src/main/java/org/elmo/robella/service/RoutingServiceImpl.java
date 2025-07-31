package org.elmo.robella.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elmo.robella.adapter.AdapterFactory;
import org.elmo.robella.adapter.AIProviderAdapter;
import org.elmo.robella.config.ProviderConfig;
import org.elmo.robella.model.request.UnifiedChatRequest;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoutingServiceImpl implements RoutingService {

    private final ProviderConfig providerConfig;
    private final AdapterFactory adapterFactory;
    
    // 缓存适配器实例
    private final Map<String, AIProviderAdapter> adapterCache = new ConcurrentHashMap<>();

    @Override
    public String decideProvider(UnifiedChatRequest request) {
        // 简单实现：根据模型名称决定提供商
        String model = request.getModel();
        
        // 检查 providers 是否为 null
        Map<String, ProviderConfig.Provider> providers = providerConfig.getProviders();
        if (providers == null) {
            // 如果 providers 为 null，默认返回 openai
            return "openai";
        }
        
        // 检查是否是特定提供商的模型
        for (ProviderConfig.Provider provider : providers.values()) {
            // 检查 provider 和其 models 是否为 null
            if (provider != null && provider.getModels() != null) {
                for (ProviderConfig.Model providerModel : provider.getModels()) {
                    if (providerModel != null && providerModel.getName().equals(model)) {
                        log.info("使用provider: {} for model: {}", provider.getName(), model);
                        return provider.getName();
                    }
                }
            }
        }
        
        // 默认返回openai
        return "openai";
    }

    @Override
    public ProviderConfig.Provider getProviderConfig(String providerName) {
        Map<String, ProviderConfig.Provider> providers = providerConfig.getProviders();
        if (providers == null) {
            throw new IllegalStateException("Providers configuration is not loaded properly");
        }
        return providers.get(providerName);
    }
    
    @Override
    public Map<String, ProviderConfig.Provider> getProviderConfigMap() {
        return providerConfig.getProviders();
    }
    
    @Override
    public AIProviderAdapter getAdapter(String providerName) {
        // 先从缓存获取
        AIProviderAdapter adapter = adapterCache.get(providerName);
        if (adapter != null) {
            return adapter;
        }
        
        // 创建新的适配器实例
        ProviderConfig.Provider config = getProviderConfig(providerName);
        if (config != null) {
            adapter = adapterFactory.createAdapter(providerName, config);
            adapterCache.put(providerName, adapter);
            return adapter;
        }
        
        throw new IllegalArgumentException("No configuration found for provider: " + providerName);
    }
}
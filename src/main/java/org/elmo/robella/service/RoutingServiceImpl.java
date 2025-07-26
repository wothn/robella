package org.elmo.robella.service;

import lombok.RequiredArgsConstructor;
import org.elmo.robella.adapter.AdapterFactory;
import org.elmo.robella.adapter.AIProviderAdapter;
import org.elmo.robella.config.ProviderConfig;
import org.elmo.robella.model.request.UnifiedChatRequest;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class RoutingServiceImpl implements RoutingService {

    private final ProviderConfig providerConfig;
    private final AdapterFactory adapterFactory;
    
    // 缓存适配器实例
    private final Map<String, AIProviderAdapter> adapterCache = new ConcurrentHashMap<>();

    @Override
    public String decideProvider(UnifiedChatRequest request) {
        // 简单实现：根据模型名称决定提供商
        String model = request.getModel();
        
        // 检查是否是特定提供商的模型
        for (ProviderConfig.Provider provider : providerConfig.getProviders().values()) {
            for (ProviderConfig.Model providerModel : provider.getModels()) {
                if (providerModel.getName().equals(model)) {
                    return provider.getName();
                }
            }
        }
        
        // 默认返回openai
        return "openai";
    }

    @Override
    public ProviderConfig.Provider getProviderConfig(String providerName) {
        return providerConfig.getProviders().get(providerName);
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
package org.elmo.robella.service;

import org.elmo.robella.adapter.AIProviderAdapter;
import org.elmo.robella.config.ProviderConfig;
import org.elmo.robella.model.request.UnifiedChatRequest;

import java.util.Map;

public interface RoutingService {
    /**
     * 根据请求决定目标提供商
     */
    String decideProvider(UnifiedChatRequest request);

    /**
     * 获取提供商配置
     */
    ProviderConfig.Provider getProviderConfig(String providerName);
    
    /**
     * 获取所有提供商配置
     */
    Map<String, ProviderConfig.Provider> getProviderConfigMap();
    
    /**
     * 获取提供商适配器
     */
    AIProviderAdapter getAdapter(String providerName);
}
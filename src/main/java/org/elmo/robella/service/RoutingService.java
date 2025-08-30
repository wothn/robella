package org.elmo.robella.service;

import org.elmo.robella.client.ApiClient;
import org.elmo.robella.config.ProviderConfig;
import org.elmo.robella.model.openai.core.ChatCompletionRequest;
import org.elmo.robella.model.openai.model.ModelListResponse;

import java.util.Map;

public interface RoutingService {

    /**
     * 根据模型名称决定提供商
     */
    String decideProviderByModel(String model);

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
    ApiClient getClient(String providerName);
    
    /**
     * 获取可用模型列表（带缓存）
     */
    ModelListResponse getAvailableModels();
    
    /**
     * 刷新模型缓存
     */
    void refreshModelCache();
    
    /**
     * 获取提供商类型
     */
    String getProviderType(String providerName);
}
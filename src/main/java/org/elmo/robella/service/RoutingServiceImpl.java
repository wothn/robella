package org.elmo.robella.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elmo.robella.adapter.AdapterFactory;
import org.elmo.robella.adapter.AIProviderAdapter;
import org.elmo.robella.config.ProviderConfig;
import org.elmo.robella.model.openai.core.ChatCompletionRequest;
import org.elmo.robella.model.openai.model.ModelInfo;
import org.elmo.robella.model.openai.model.ModelListResponse;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoutingServiceImpl implements RoutingService {

    private final ProviderConfig providerConfig;
    private final AdapterFactory adapterFactory;
    
    // 缓存适配器实例
    private final Map<String, AIProviderAdapter> adapterCache = new ConcurrentHashMap<>();
    
    // 缓存模型列表，使用 AtomicReference 保证线程安全
    private final AtomicReference<ModelListResponse> cachedModels = new AtomicReference<>();
    
    /**
     * 初始化时预构建模型列表缓存
     */
    @PostConstruct
    private void initModelCache() {
        try {
            ModelListResponse models = buildModelList();
            cachedModels.set(models);
            log.info("Model cache initialized with {} models", models.getData().size());
        } catch (Exception e) {
            log.error("Failed to initialize model cache", e);
        }
    }
    
    /**
     * 构建模型列表（内部方法）
     */
    private ModelListResponse buildModelList() {
        // 获取所有提供商配置
        Map<String, ProviderConfig.Provider> providers = getProviderConfigMap();
        
        // 从配置中收集所有模型
        ModelListResponse models = new ModelListResponse("list", new ArrayList<>());

        if (providers != null) {
            for (var provider : providers.values()) {
                if (provider.getModels() == null) {
                    continue;
                }
                for (var model : provider.getModels()) {
                    models.getData().add(new ModelInfo(model.getName(), "model", provider.getName()));
                }
            }
        }
        
        return models;
    }

    @Override
    public String decideProvider(ChatCompletionRequest request) {
        // 简单实现：根据模型名称决定提供商
        String model = request.getModel();
        return decideProviderByModel(model);
    }

    @Override
    public String decideProviderByModel(String model) {
        Map<String, ProviderConfig.Provider> providers = providerConfig.getProviders();
        if (providers == null || model == null) return "openai";
        for (ProviderConfig.Provider provider : providers.values()) {
            if (provider != null && provider.getModels() != null) {
                for (ProviderConfig.Model providerModel : provider.getModels()) {
                    if (providerModel != null && model.equals(providerModel.getName())) {
                        log.info("使用provider: {} for model: {}", provider.getName(), model);
                        return provider.getName();
                    }
                }
            }
        }
        return "openai"; // 默认
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
    
    @Override
    public ModelListResponse getAvailableModels() {
        // 从缓存获取模型列表
        ModelListResponse cached = cachedModels.get();
        
        if (cached != null) {
            log.debug("Returning cached model list with {} models", cached.getData().size());
            return cached;
        }
        
        // 如果缓存为空，重新构建并缓存
        log.warn("Model cache is empty, rebuilding...");
        ModelListResponse models = buildModelList();
        cachedModels.set(models);
        
        return models;
    }
    
    @Override
    public void refreshModelCache() {
        log.info("Refreshing model cache...");
        try {
            ModelListResponse models = buildModelList();
            cachedModels.set(models);
            log.info("Model cache refreshed successfully with {} models", models.getData().size());
        } catch (Exception e) {
            log.error("Failed to refresh model cache", e);
            throw new RuntimeException("Failed to refresh model cache", e);
        }
    }
}
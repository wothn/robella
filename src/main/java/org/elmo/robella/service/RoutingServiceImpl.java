package org.elmo.robella.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elmo.robella.client.ClientFactory;
import org.elmo.robella.client.ApiClient;
import org.elmo.robella.config.ProviderConfig;
import org.elmo.robella.model.Provider;
import org.elmo.robella.model.Model;
import org.elmo.robella.model.openai.core.ChatCompletionRequest;
import org.elmo.robella.model.openai.model.ModelInfo;
import org.elmo.robella.model.openai.model.ModelListResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class RoutingServiceImpl implements RoutingService {

    @Autowired
    private ProviderConfig providerConfig;
    
    @Autowired
    private ClientFactory clientFactory;
    
    @Autowired
    private ProviderService providerService;
    
    // 缓存适配器实例
    private final Map<String, ApiClient> adapterCache = new ConcurrentHashMap<>();
    
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
        // 从数据库中获取所有活跃的模型
        ModelListResponse models = new ModelListResponse("list");
        
        try {
            List<Provider> providers = providerService.getActiveProviders().collectList().block();
            List<Model> allModels = providerService.getAllActiveModels().collectList().block();
            
            if (providers != null && allModels != null) {
                for (Model model : allModels) {
                    // 找到对应的provider
                    for (Provider provider : providers) {
                        if (model.getProviderId().equals(provider.getId())) {
                            models.getData().add(new ModelInfo(model.getName(), "model", provider.getName()));
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to build model list from database", e);
        }
        
        return models;
    }


    @Override
    public String decideProviderByModel(String model) {
        if (model == null) return "openai";
        
        try {
            // 从数据库查找模型对应的provider
            List<Model> models = providerService.getAllActiveModels()
                    .filter(m -> m.getName().equals(model))
                    .collectList()
                    .block();
            
            if (models != null && !models.isEmpty()) {
                Model foundModel = models.get(0);
                Provider provider = providerService.getProviderById(foundModel.getProviderId()).block();
                if (provider != null) {
                    log.info("使用provider: {} for model: {}", provider.getName(), model);
                    return provider.getName();
                }
            }
        } catch (Exception e) {
            log.error("Failed to decide provider by model from database", e);
        }
        
        return "openai"; // 默认
    }

    @Override
    public ProviderConfig.Provider getProviderConfig(String providerName) {
        // 从数据库获取provider配置
        try {
            Provider provider = providerService.getActiveProviderByName(providerName).block();
            if (provider != null) {
                ProviderConfig.Provider config = new ProviderConfig.Provider();
                config.setName(provider.getName());
                config.setType(provider.getType());
                config.setApiKey(provider.getApiKey());
                config.setBaseUrl(provider.getBaseUrl());
                config.setDeploymentName(provider.getDeploymentName());
                return config;
            }
        } catch (Exception e) {
            log.error("Failed to get provider config from database", e);
        }
        
        throw new IllegalArgumentException("No configuration found for provider: " + providerName);
    }
    
    @Override
    public Map<String, ProviderConfig.Provider> getProviderConfigMap() {
        // 返回空map，因为我们现在使用数据库
        return Map.of();
    }
    
    @Override
    public ApiClient getClient(String providerName) {
        // 先从缓存获取
        ApiClient adapter = adapterCache.get(providerName);
        if (adapter != null) {
            return adapter;
        }
        
        // 创建新的适配器实例
        ProviderConfig.Provider config = getProviderConfig(providerName);
        if (config != null) {
            adapter = clientFactory.createClient(providerName, config);
            adapterCache.put(providerName, adapter);
            return adapter;
        }
        
        throw new IllegalArgumentException("No configuration found for provider: " + providerName);
    }
    
    @Override
    public String getProviderType(String providerName) {
        try {
            Provider provider = providerService.getActiveProviderByName(providerName).block();
            if (provider != null && provider.getType() != null) {
                return provider.getType();
            }
        } catch (Exception e) {
            log.error("Failed to get provider type from database", e);
        }
        return "OpenAI"; // 默认类型
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
package org.elmo.robella.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elmo.robella.client.openai.OpenAIClient;
import org.elmo.robella.client.anthropic.AnthropicClient;
import org.elmo.robella.config.WebClientProperties;
import org.elmo.robella.model.common.EndpointType;
import org.elmo.robella.model.entity.Provider;
import org.elmo.robella.repository.ProviderRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClientFactory {

    private final WebClient defaultWebClient;
    private final WebClientProperties webClientProperties;
    private final ProviderRepository providerRepository;
    
    // 缓存适配器实例
    private final Map<String, ApiClient> clientCache = new ConcurrentHashMap<>();

    public ApiClient getClient(String providerName) {
        // 先从缓存获取
        ApiClient client = clientCache.get(providerName);
        if (client != null) {
            log.debug("Retrieved cached client for provider: {}", providerName);
            return client;
        }
        // 获取提供商信息
        Provider provider = providerRepository.findByName(providerName).block();
        // 创建新的客户端实例
        EndpointType type = provider.getType();
        log.debug("Creating client for provider: {}, type: {}", providerName, type);
        
        client = switch (type) {
            case OpenAI -> {
                log.debug("Creating OpenAI client for provider: {}", providerName);
                yield new OpenAIClient(provider, defaultWebClient, webClientProperties);
            }
            case Anthropic -> {
                log.debug("Creating Anthropic client for provider: {}", providerName);
                yield new AnthropicClient(provider, defaultWebClient, webClientProperties);
            }
        };
        
        // 放入缓存
        clientCache.put(providerName, client);
        return client;
    }
    
    public void clearCache() {
        log.debug("Clearing client cache");
        clientCache.clear();
    }
    
    public void removeFromCache(String providerName) {
        log.debug("Removing client from cache for provider: {}", providerName);
        clientCache.remove(providerName);
    }
}
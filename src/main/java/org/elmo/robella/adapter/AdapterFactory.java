package org.elmo.robella.adapter;

import lombok.extern.slf4j.Slf4j;
import org.elmo.robella.adapter.claude.ClaudeAdapter;
import org.elmo.robella.adapter.gemini.GeminiAdapter;
import org.elmo.robella.adapter.openai.OpenAIAdapter;
import org.elmo.robella.config.ProviderConfig;
import org.elmo.robella.config.WebClientProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Component
public class AdapterFactory {
    
    private final ProviderConfig providerConfig;
    private final WebClient defaultWebClient;
    private final WebClient retryableWebClient;
    private final WebClientProperties webClientProperties;
    
    public AdapterFactory(ProviderConfig providerConfig, 
                         WebClient.Builder webClientBuilder,
                         @Qualifier("retryableWebClient") WebClient retryableWebClient,
                         WebClientProperties webClientProperties) {
        this.providerConfig = providerConfig;
        this.defaultWebClient = webClientBuilder.build();
        this.retryableWebClient = retryableWebClient;
        this.webClientProperties = webClientProperties;
    }
    
    public AIProviderAdapter createAdapter(String providerName, ProviderConfig.Provider config) {
        String type = config.getType();
        if (type == null) {
            type = providerName; // 默认使用providerName作为type
        }
        
        log.debug("Creating adapter for provider: {}, type: {}", providerName, type);
        
        return switch (type) {
            case "OpenAI", "AzureOpenAI" -> {
                log.debug("Creating OpenAI adapter for provider: {}", providerName);
                yield new OpenAIAdapter(config, defaultWebClient, webClientProperties);
            }
            case "Anthropic" -> {
                log.debug("Creating Claude adapter for provider: {}", providerName);
                yield new ClaudeAdapter(providerConfig, defaultWebClient, webClientProperties);
            }
            case "Gemini" -> {
                log.debug("Creating Gemini adapter for provider: {}", providerName);
                yield new GeminiAdapter(providerConfig, defaultWebClient, webClientProperties);
            }
            default -> {
                log.debug("Creating default OpenAI-compatible adapter for provider: {}", providerName);
                yield new OpenAIAdapter(config, defaultWebClient, webClientProperties);
            }
        };
    }
}
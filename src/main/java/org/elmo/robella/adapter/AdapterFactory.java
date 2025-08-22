package org.elmo.robella.adapter;

import lombok.extern.slf4j.Slf4j;
import org.elmo.robella.adapter.openai.OpenAIAdapter;
import org.elmo.robella.adapter.anthropic.AnthropicAdapter;
import org.elmo.robella.config.ProviderConfig;
import org.elmo.robella.config.ProviderType;
import org.elmo.robella.config.WebClientProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Component
public class AdapterFactory {
    
    private final ProviderConfig providerConfig;
    private final WebClient defaultWebClient;
    private final WebClientProperties webClientProperties;
    
    public AdapterFactory(ProviderConfig providerConfig, 
                         WebClient.Builder webClientBuilder,
                         WebClientProperties webClientProperties) {
        this.providerConfig = providerConfig;
        this.defaultWebClient = webClientBuilder.build();
        this.webClientProperties = webClientProperties;
    }
    
    public AIProviderAdapter createAdapter(String providerName, ProviderConfig.Provider config) {
        ProviderType type = config.getProviderType();
        log.debug("Creating adapter for provider: {}, type: {}", providerName, type);
        return switch (type) {
            case OpenAI, AzureOpenAI -> {
                log.debug("Creating OpenAI adapter for provider: {}", providerName);
                yield new OpenAIAdapter(config, defaultWebClient, webClientProperties);
            }
            case Anthropic -> {
                log.debug("Creating Anthropic adapter for provider: {}", providerName);
                yield new AnthropicAdapter(config, defaultWebClient, webClientProperties);
            }
        };
    }
}
package org.elmo.robella.adapter;

import lombok.extern.slf4j.Slf4j;
import org.elmo.robella.adapter.openai.OpenAIClient;
import org.elmo.robella.adapter.anthropic.AnthropicClient;
import org.elmo.robella.config.ProviderConfig;
import org.elmo.robella.config.ProviderType;
import org.elmo.robella.config.WebClientProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Component
public class ClientFactory {
    
    private final ProviderConfig providerConfig;
    private final WebClient defaultWebClient;
    private final WebClientProperties webClientProperties;
    
    public ClientFactory(ProviderConfig providerConfig,
                         WebClient.Builder webClientBuilder,
                         WebClientProperties webClientProperties) {
        this.providerConfig = providerConfig;
        this.defaultWebClient = webClientBuilder.build();
        this.webClientProperties = webClientProperties;
    }
    
    public ApiClient createAdapter(String providerName, ProviderConfig.Provider config) {
        ProviderType type = config.getProviderType();
        log.debug("Creating adapter for provider: {}, type: {}", providerName, type);
        return switch (type) {
            case OpenAI, AzureOpenAI -> {
                log.debug("Creating OpenAI adapter for provider: {}", providerName);
                yield new OpenAIClient(config, defaultWebClient, webClientProperties);
            }
            case Anthropic -> {
                log.debug("Creating Anthropic adapter for provider: {}", providerName);
                yield new AnthropicClient(config, defaultWebClient, webClientProperties);
            }
        };
    }
}
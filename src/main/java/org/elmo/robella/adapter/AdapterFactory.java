package org.elmo.robella.adapter;

import org.elmo.robella.adapter.claude.ClaudeAdapter;
import org.elmo.robella.adapter.gemini.GeminiAdapter;
import org.elmo.robella.adapter.openai_compatible.OpenAICompatibleAdapter;
import org.elmo.robella.config.ProviderConfig;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class AdapterFactory {
    
    private final ProviderConfig providerConfig;
    private final WebClient.Builder webClientBuilder;
    
    public AdapterFactory(ProviderConfig providerConfig, WebClient.Builder webClientBuilder) {
        this.providerConfig = providerConfig;
        this.webClientBuilder = webClientBuilder;
    }
    
    public AIProviderAdapter createAdapter(String providerName, ProviderConfig.Provider config) {
        String type = config.getType();
        if (type == null) {
            type = providerName; // 默认使用providerName作为type
        }
        
        switch (type) {
            case "OpenAI":
                return new OpenAICompatibleAdapter(config, webClientBuilder.build());
            case "AzureOpenAI":
                return new OpenAICompatibleAdapter(config, webClientBuilder.build()); // Azure OpenAI也是OpenAI兼容的
            case "Anthropic":
                return new ClaudeAdapter(providerConfig, webClientBuilder);
            case "Gemini":
                return new GeminiAdapter(providerConfig, webClientBuilder);
            default:
                // 默认使用OpenAI兼容适配器
                return new OpenAICompatibleAdapter(config, webClientBuilder.build());
        }
    }
}
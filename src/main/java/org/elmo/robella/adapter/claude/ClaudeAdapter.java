package org.elmo.robella.adapter.claude;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elmo.robella.adapter.AIProviderAdapter;
import org.elmo.robella.config.ProviderConfig;
import org.elmo.robella.model.common.ModelInfo;
import org.elmo.robella.model.response.ClaudeChatResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClaudeAdapter implements AIProviderAdapter {

    private final ProviderConfig providerConfig;
    private final WebClient.Builder webClientBuilder;

    @Override
    public Mono<?> chatCompletion(Object request) {
        ProviderConfig.Provider provider = providerConfig.getProviders().get("claude");
        if (provider == null) {
            return Mono.error(new RuntimeException("Claude provider not configured"));
        }

        WebClient webClient = webClientBuilder
                .clone()
                .baseUrl(provider.getBaseUrl())
                .defaultHeader("x-api-key", provider.getApiKey())
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("anthropic-version", "2023-06-01")
                .build();

        return webClient.post()
                .uri("/messages")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ClaudeChatResponse.class);
    }

    @Override
    public Flux<?> streamChatCompletion(Object request) {
        ProviderConfig.Provider provider = providerConfig.getProviders().get("claude");
        if (provider == null) {
            return Flux.error(new RuntimeException("Claude provider not configured"));
        }

        WebClient webClient = webClientBuilder
                .clone()
                .baseUrl(provider.getBaseUrl())
                .defaultHeader("x-api-key", provider.getApiKey())
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("anthropic-version", "2023-06-01")
                .build();

        return webClient.post()
                .uri("/messages")
                .bodyValue(request)
                .retrieve()
                .bodyToFlux(String.class)
                .map(data -> {
                    // 处理SSE数据
                    return data;
                });
    }

    @Override
    public Mono<List<ModelInfo>> listModels() {
        // Claude API没有直接的模型列表接口，我们返回预定义的模型列表
        ProviderConfig.Provider provider = providerConfig.getProviders().get("claude");
        if (provider == null) {
            return Mono.error(new RuntimeException("Claude provider not configured"));
        }

        List<ModelInfo> models = provider.getModels().stream()
                .map(model -> {
                    ModelInfo info = new ModelInfo();
                    info.setId(model.getName());
                    info.setVendor("claude");
                    return info;
                })
                .collect(Collectors.toList());

        return Mono.just(models);
    }

    @Override
    public String getProviderName() {
        return "claude";
    }
}
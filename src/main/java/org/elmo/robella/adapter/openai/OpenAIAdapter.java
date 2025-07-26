package org.elmo.robella.adapter.openai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elmo.robella.adapter.AIProviderAdapter;
import org.elmo.robella.config.ProviderConfig;
import org.elmo.robella.model.common.ModelInfo;
import org.elmo.robella.model.request.OpenAIChatRequest;
import org.elmo.robella.model.response.OpenAIChatResponse;
import org.elmo.robella.model.response.OpenAIModelListResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAIAdapter implements AIProviderAdapter {

    private final ProviderConfig providerConfig;
    private final WebClient.Builder webClientBuilder;

    @Override
    public Mono<?> chatCompletion(Object request) {
        ProviderConfig.Provider provider = providerConfig.getProviders().get("openai");
        if (provider == null) {
            return Mono.error(new RuntimeException("OpenAI provider not configured"));
        }

        WebClient webClient = webClientBuilder
                .clone()
                .baseUrl(provider.getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + provider.getApiKey())
                .defaultHeader("Content-Type", "application/json")
                .build();

        return webClient.post()
                .uri("/chat/completions")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(OpenAIChatResponse.class);
    }

    @Override
    public Flux<?> streamChatCompletion(Object request) {
        ProviderConfig.Provider provider = providerConfig.getProviders().get("openai");
        if (provider == null) {
            return Flux.error(new RuntimeException("OpenAI provider not configured"));
        }

        WebClient webClient = webClientBuilder
                .clone()
                .baseUrl(provider.getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + provider.getApiKey())
                .defaultHeader("Content-Type", "application/json")
                .build();

        return webClient.post()
                .uri("/chat/completions")
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
        ProviderConfig.Provider provider = providerConfig.getProviders().get("openai");
        if (provider == null) {
            return Mono.error(new RuntimeException("OpenAI provider not configured"));
        }

        WebClient webClient = webClientBuilder
                .clone()
                .baseUrl(provider.getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + provider.getApiKey())
                .build();

        return webClient.get()
                .uri("/models")
                .retrieve()
                .bodyToMono(OpenAIModelListResponse.class)
                .map(response -> response.getData().stream()
                        .map(model -> {
                            ModelInfo info = new ModelInfo();
                            info.setId(model.getId());
                            info.setVendor("openai");
                            return info;
                        })
                        .collect(Collectors.toList()));
    }

    @Override
    public String getProviderName() {
        return "openai";
    }
}
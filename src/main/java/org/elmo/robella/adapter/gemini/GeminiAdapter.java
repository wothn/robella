package org.elmo.robella.adapter.gemini;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elmo.robella.adapter.AIProviderAdapter;
import org.elmo.robella.config.ProviderConfig;
import org.elmo.robella.model.common.ModelInfo;
import org.elmo.robella.model.response.GeminiChatResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class GeminiAdapter implements AIProviderAdapter {

    private final ProviderConfig providerConfig;
    private final WebClient.Builder webClientBuilder;

    @Override
    public Mono<?> chatCompletion(Object request) {
        ProviderConfig.Provider provider = providerConfig.getProviders().get("gemini");
        if (provider == null) {
            return Mono.error(new RuntimeException("Gemini provider not configured"));
        }

        WebClient webClient = webClientBuilder
                .clone()
                .baseUrl(provider.getBaseUrl())
                .defaultHeader("Content-Type", "application/json")
                .build();

        String apiKey = provider.getApiKey();
        String model = "gemini-pro"; // 默认模型

        return webClient.post()
                .uri("/models/{model}:generateContent?key={apiKey}", model, apiKey)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(GeminiChatResponse.class);
    }

    @Override
    public Flux<?> streamChatCompletion(Object request) {
        ProviderConfig.Provider provider = providerConfig.getProviders().get("gemini");
        if (provider == null) {
            return Flux.error(new RuntimeException("Gemini provider not configured"));
        }

        WebClient webClient = webClientBuilder
                .clone()
                .baseUrl(provider.getBaseUrl())
                .defaultHeader("Content-Type", "application/json")
                .build();

        String apiKey = provider.getApiKey();
        String model = "gemini-pro"; // 默认模型

        return webClient.post()
                .uri("/models/{model}:streamGenerateContent?key={apiKey}", model, apiKey)
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
        // Gemini API没有直接的模型列表接口，我们返回预定义的模型列表
        ProviderConfig.Provider provider = providerConfig.getProviders().get("gemini");
        if (provider == null) {
            return Mono.error(new RuntimeException("Gemini provider not configured"));
        }

        List<ModelInfo> models = provider.getModels().stream()
                .map(model -> {
                    ModelInfo info = new ModelInfo();
                    info.setId(model.getName());
                    info.setVendor("gemini");
                    return info;
                })
                .collect(Collectors.toList());

        return Mono.just(models);
    }

    @Override
    public String getProviderName() {
        return "gemini";
    }
}
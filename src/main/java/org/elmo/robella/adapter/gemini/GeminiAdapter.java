package org.elmo.robella.adapter.gemini;

import lombok.extern.slf4j.Slf4j;
import org.elmo.robella.adapter.AIProviderAdapter;
import org.elmo.robella.config.ProviderConfig;
import org.elmo.robella.config.WebClientProperties;
import org.elmo.robella.exception.ProviderException;
import org.elmo.robella.model.common.ModelInfo;
import org.elmo.robella.model.response.GeminiChatResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

@Slf4j
public class GeminiAdapter implements AIProviderAdapter {

    private final ProviderConfig providerConfig;
    private final WebClient webClient;
    private final WebClientProperties webClientProperties;

    public GeminiAdapter(ProviderConfig providerConfig, WebClient webClient, WebClientProperties webClientProperties) {
        this.providerConfig = providerConfig;
        this.webClient = webClient;
        this.webClientProperties = webClientProperties;
    }

    @Override
    public Mono<?> chatCompletion(Object request) {
        ProviderConfig.Provider provider = providerConfig.getProviders().get("gemini");
        if (provider == null) {
            return Mono.error(new ProviderException("Gemini provider not configured"));
        }

        WebClient geminiClient = webClient.mutate()
                .baseUrl(provider.getBaseUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.USER_AGENT, "Robella-AI-Proxy/1.0")
                .build();

        String apiKey = provider.getApiKey();
        String model = "gemini-pro"; // 默认模型，实际应从请求中获取

        log.debug("Sending chat completion request to Gemini API, model: {}", model);

        return geminiClient.post()
                .uri("/models/{model}:generateContent?key={apiKey}", model, apiKey)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(GeminiChatResponse.class)
                .timeout(webClientProperties.getTimeout().getRead()) // 使用配置的读超时
                .doOnSuccess(response -> log.debug("Successfully received response from Gemini"))
                .onErrorMap(WebClientResponseException.class, this::handleWebClientError)
                .onErrorMap(Exception.class, e -> new ProviderException(
                    "Gemini API call failed: " + e.getMessage(), e));
    }

    @Override
    public Flux<?> streamChatCompletion(Object request) {
        ProviderConfig.Provider provider = providerConfig.getProviders().get("gemini");
        if (provider == null) {
            return Flux.error(new ProviderException("Gemini provider not configured"));
        }

        WebClient geminiClient = webClient.mutate()
                .baseUrl(provider.getBaseUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, "text/event-stream")
                .defaultHeader(HttpHeaders.USER_AGENT, "Robella-AI-Proxy/1.0")
                .build();

        String apiKey = provider.getApiKey();
        String model = "gemini-pro"; // 默认模型

        log.debug("Sending streaming chat completion request to Gemini API, model: {}", model);

        // 流式请求通常需要更长的超时时间，使用读超时的5倍
        Duration streamTimeout = webClientProperties.getTimeout().getRead().multipliedBy(5);
        
        return geminiClient.post()
                .uri("/models/{model}:streamGenerateContent?key={apiKey}", model, apiKey)
                .bodyValue(request)
                .retrieve()
                .bodyToFlux(String.class)
                .timeout(streamTimeout) // 使用配置化的流式超时
                .doOnNext(data -> log.trace("Received streaming data from Gemini: {}", data))
                .doOnComplete(() -> log.debug("Gemini streaming completed"))
                .onErrorMap(WebClientResponseException.class, this::handleWebClientError)
                .onErrorMap(Exception.class, e -> new ProviderException(
                    "Gemini streaming API call failed: " + e.getMessage(), e));
    }

    @Override
    public Mono<List<ModelInfo>> listModels() {
        // Gemini API没有直接的模型列表接口，返回预定义的模型列表
        ProviderConfig.Provider provider = providerConfig.getProviders().get("gemini");
        if (provider == null) {
            return Mono.error(new ProviderException("Gemini provider not configured"));
        }

        if (provider.getModels() == null) {
            return Mono.just(Collections.emptyList());
        }

        List<ModelInfo> models = provider.getModels().stream()
                .map(model -> {
                    ModelInfo info = new ModelInfo();
                    info.setId(model.getName());
                    info.setName(model.getName());
                    info.setVendor("gemini");
                    return info;
                })
                .toList();

        log.debug("Returning {} configured Gemini models", models.size());
        return Mono.just(models);
    }

    @Override
    public String getProviderName() {
        return "gemini";
    }

    /**
     * 处理WebClient异常
     */
    private ProviderException handleWebClientError(WebClientResponseException ex) {
        String errorMessage = String.format("Gemini API error: %d %s", 
                ex.getStatusCode().value(), ex.getStatusText());
        
        if (ex.getResponseBodyAsString() != null && !ex.getResponseBodyAsString().isEmpty()) {
            errorMessage += " - " + ex.getResponseBodyAsString();
        }
        
        log.error("Gemini API call failed: {}", errorMessage);
        return new ProviderException(errorMessage, ex);
    }
}
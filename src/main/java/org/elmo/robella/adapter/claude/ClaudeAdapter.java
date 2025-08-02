package org.elmo.robella.adapter.claude;

import lombok.extern.slf4j.Slf4j;
import org.elmo.robella.adapter.AIProviderAdapter;
import org.elmo.robella.config.ProviderConfig;
import org.elmo.robella.config.WebClientProperties;
import org.elmo.robella.exception.ProviderException;
import org.elmo.robella.model.openai.ModelInfo;
import org.elmo.robella.model.response.ClaudeChatResponse;
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
public class ClaudeAdapter implements AIProviderAdapter {

    private final ProviderConfig providerConfig;
    private final WebClient webClient;
    private final WebClientProperties webClientProperties;

    public ClaudeAdapter(ProviderConfig providerConfig, WebClient webClient, WebClientProperties webClientProperties) {
        this.providerConfig = providerConfig;
        this.webClient = webClient;
        this.webClientProperties = webClientProperties;
    }

    @Override
    public Mono<?> chatCompletion(Object request) {
        ProviderConfig.Provider provider = providerConfig.getProviders().get("claude");
        if (provider == null) {
            return Mono.error(new ProviderException("Claude provider not configured"));
        }

        WebClient claudeClient = webClient.mutate()
                .baseUrl(provider.getBaseUrl())
                .defaultHeader("x-api-key", provider.getApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("anthropic-version", "2023-06-01")
                .defaultHeader(HttpHeaders.USER_AGENT, "Robella-AI-Proxy/1.0")
                .build();

        log.debug("Sending chat completion request to Claude API");

        return claudeClient.post()
                .uri("/messages")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ClaudeChatResponse.class)
                .timeout(webClientProperties.getTimeout().getRead()) // 使用配置的读超时
                .doOnSuccess(response -> log.debug("Successfully received response from Claude"))
                .onErrorMap(WebClientResponseException.class, this::handleWebClientError)
                .onErrorMap(Exception.class, e -> new ProviderException(
                    "Claude API call failed: " + e.getMessage(), e));
    }

    @Override
    public Flux<?> streamChatCompletion(Object request) {
        ProviderConfig.Provider provider = providerConfig.getProviders().get("claude");
        if (provider == null) {
            return Flux.error(new ProviderException("Claude provider not configured"));
        }

        WebClient claudeClient = webClient.mutate()
                .baseUrl(provider.getBaseUrl())
                .defaultHeader("x-api-key", provider.getApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("anthropic-version", "2023-06-01")
                .defaultHeader(HttpHeaders.ACCEPT, "text/event-stream")
                .defaultHeader(HttpHeaders.USER_AGENT, "Robella-AI-Proxy/1.0")
                .build();

        log.debug("Sending streaming chat completion request to Claude API");

        // 流式请求通常需要更长的超时时间，使用读超时的5倍
        Duration streamTimeout = webClientProperties.getTimeout().getRead().multipliedBy(5);

        return claudeClient.post()
                .uri("/messages")
                .bodyValue(request)
                .retrieve()
                .bodyToFlux(String.class)
                .timeout(streamTimeout) // 使用配置化的流式超时
                .doOnNext(data -> log.trace("Received streaming data from Claude: {}", data))
                .doOnComplete(() -> log.debug("Claude streaming completed"))
                .onErrorMap(WebClientResponseException.class, this::handleWebClientError)
                .onErrorMap(Exception.class, e -> new ProviderException(
                    "Claude streaming API call failed: " + e.getMessage(), e));
    }

    @Override
    public Mono<List<ModelInfo>> listModels() {
        // Claude API没有直接的模型列表接口，返回预定义的模型列表
        ProviderConfig.Provider provider = providerConfig.getProviders().get("claude");
        if (provider == null) {
            return Mono.error(new ProviderException("Claude provider not configured"));
        }

        if (provider.getModels() == null) {
            return Mono.just(Collections.emptyList());
        }

        List<ModelInfo> models = provider.getModels().stream()
                .map(model -> {
                    ModelInfo info = new ModelInfo();
                    info.setId(model.getName());
                    info.setObject("model");
                    info.setOwnedBy("claude");
                    return info;
                })
                .toList();

        log.debug("Returning {} configured Claude models", models.size());
        return Mono.just(models);
    }

    @Override
    public String getProviderName() {
        return "claude";
    }

    /**
     * 处理WebClient异常
     */
    private ProviderException handleWebClientError(WebClientResponseException ex) {
        String errorMessage = String.format("Claude API error: %d %s", 
                ex.getStatusCode().value(), ex.getStatusText());
        
        if (ex.getResponseBodyAsString() != null && !ex.getResponseBodyAsString().isEmpty()) {
            errorMessage += " - " + ex.getResponseBodyAsString();
        }
        
        log.error("Claude API call failed: {}", errorMessage);
        return new ProviderException(errorMessage, ex);
    }
}
package org.elmo.robella.adapter.anthropic;

import lombok.extern.slf4j.Slf4j;
import org.elmo.robella.adapter.AIProviderAdapter;
import org.elmo.robella.config.ProviderConfig;
import org.elmo.robella.config.WebClientProperties;
import org.elmo.robella.exception.ProviderException;
import org.elmo.robella.model.anthropic.MessageResponse;
import org.elmo.robella.model.openai.ModelInfo;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

/**
 * Anthropic Messages API 适配器 (替换原 ClaudeAdapter)。
 */
@Slf4j
public class AnthropicAdapter implements AIProviderAdapter {

    private final ProviderConfig providerConfig;
    private final WebClient webClient;
    private final WebClientProperties webClientProperties;

    public AnthropicAdapter(ProviderConfig providerConfig, WebClient webClient, WebClientProperties webClientProperties) {
        this.providerConfig = providerConfig;
        this.webClient = webClient;
        this.webClientProperties = webClientProperties;
    }

    private ProviderConfig.Provider getProvider() {
        return providerConfig.getProviders().get("claude");
    }

    private WebClient buildClient(boolean stream) {
        ProviderConfig.Provider provider = getProvider();
        if (provider == null) {
            throw new ProviderException("Anthropic (claude) provider not configured");
        }
        WebClient.Builder builder = webClient.mutate()
                .baseUrl(provider.getBaseUrl())
                .defaultHeader("x-api-key", provider.getApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("anthropic-version", "2023-06-01")
                .defaultHeader(HttpHeaders.USER_AGENT, "Robella-AI-Proxy/1.0");
        if (stream) {
            builder.defaultHeader(HttpHeaders.ACCEPT, "text/event-stream");
        }
        return builder.build();
    }

    @Override
    public Mono<?> chatCompletion(Object request) {
        if (getProvider() == null) {
            return Mono.error(new ProviderException("Anthropic (claude) provider not configured"));
        }
        log.debug("Sending Messages API request to Anthropic");
        return buildClient(false)
                .post()
                .uri("/messages")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(MessageResponse.class)
                .timeout(webClientProperties.getTimeout().getRead())
                .doOnSuccess(r -> log.debug("Anthropic response received"))
                .onErrorMap(WebClientResponseException.class, this::handleWebClientError)
                .onErrorMap(Exception.class, e -> new ProviderException("Anthropic API call failed: " + e.getMessage(), e));
    }

    @Override
    public Flux<?> streamChatCompletion(Object request) {
        if (getProvider() == null) {
            return Flux.error(new ProviderException("Anthropic (claude) provider not configured"));
        }
        log.debug("Sending streaming Messages API request to Anthropic");
        Duration streamTimeout = webClientProperties.getTimeout().getRead().multipliedBy(5);
        return buildClient(true)
                .post()
                .uri("/messages")
                .bodyValue(request)
                .retrieve()
                .bodyToFlux(String.class)
                .timeout(streamTimeout)
                .doOnNext(ev -> log.trace("Anthropic SSE: {}", ev))
                .doOnComplete(() -> log.debug("Anthropic streaming completed"))
                .onErrorMap(WebClientResponseException.class, this::handleWebClientError)
                .onErrorMap(Exception.class, e -> new ProviderException("Anthropic streaming failed: " + e.getMessage(), e));
    }

    @Override
    public Mono<List<ModelInfo>> listModels() {
        ProviderConfig.Provider provider = getProvider();
        if (provider == null) {
            return Mono.error(new ProviderException("Anthropic (claude) provider not configured"));
        }
        if (provider.getModels() == null) {
            return Mono.just(Collections.emptyList());
        }
        List<ModelInfo> models = provider.getModels().stream().map(m -> {
            ModelInfo info = new ModelInfo();
            info.setId(m.getName());
            info.setObject("model");
            info.setOwnedBy("anthropic");
            return info;
        }).toList();
        return Mono.just(models);
    }

    @Override
    public String getProviderName() {
        return "claude"; // 与配置键保持一致
    }

    private ProviderException handleWebClientError(WebClientResponseException ex) {
        String msg = String.format("Anthropic API error: %d %s", ex.getStatusCode().value(), ex.getStatusText());
        if (ex.getResponseBodyAsString() != null && !ex.getResponseBodyAsString().isEmpty()) {
            msg += " - " + ex.getResponseBodyAsString();
        }
        log.error("Anthropic API call failed: {}", msg);
        return new ProviderException(msg, ex);
    }
}

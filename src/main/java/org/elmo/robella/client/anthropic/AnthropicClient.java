package org.elmo.robella.client.anthropic;

import lombok.extern.slf4j.Slf4j;
import org.elmo.robella.client.ApiClient;
import org.elmo.robella.model.entity.Provider;
import org.elmo.robella.config.WebClientProperties;
import org.elmo.robella.exception.ProviderException;
import org.elmo.robella.exception.QuotaExceededException;
import org.elmo.robella.exception.RateLimitException;
import org.elmo.robella.model.anthropic.core.AnthropicChatRequest;
import org.elmo.robella.model.anthropic.core.AnthropicMessage;
import org.elmo.robella.model.anthropic.stream.AnthropicStreamEvent;
import org.elmo.robella.model.openai.model.ModelInfo;
import org.elmo.robella.service.ProviderService;
import org.elmo.robella.util.JsonUtils;
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
 * Anthropic Messages API 适配器
 */
@Slf4j
public class AnthropicClient implements ApiClient {

    private static final String SSE_DATA_PREFIX = "data: ";

    private final Provider config;
    private final WebClient webClient;
    private final WebClientProperties webClientProperties;
    private final ProviderService providerService;
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    public AnthropicClient(Provider config,
                           WebClient baseClient,
                           WebClientProperties webClientProperties) {
        this.providerService = null; // This would need to be injected properly
        this.config = config;
        this.webClient = baseClient.mutate()
                .defaultHeader("x-api-key", config.getApiKey())
                .defaultHeader("anthropic-version", ANTHROPIC_VERSION)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.USER_AGENT, "Robella")
                .build();
        this.webClientProperties = webClientProperties;
    }

    @Override
    public Mono<AnthropicMessage> chatCompletion(Object request) {
        if (!(request instanceof AnthropicChatRequest anthropicRequest)) {
            return Mono.error(new ProviderException("Invalid request type for AnthropicAdapter: "
                    + (request == null ? "null" : request.getClass().getName())));
        }

        // URL 末尾统一 /v1/messages
        String url = buildMessagesUrl();

        if (log.isDebugEnabled()) {
            log.debug("[AnthropicAdapter] chatCompletion start provider={} model={} stream=false", config.getName(),
                    anthropicRequest.getModel());
        }

        return webClient.post()
                .uri(url)
                .bodyValue(anthropicRequest)
                .retrieve()
                .bodyToMono(String.class)
                .mapNotNull(json -> {
                    try {
                        return JsonUtils.fromJson(json, AnthropicMessage.class);
                    } catch (Exception e) {
                        throw new ProviderException("Failed to deserialize Anthropic response: " + e.getMessage(), e);
                    }
                })
                .timeout(webClientProperties.getTimeout().getRead())
                .onErrorMap(ex -> mapToProviderException(ex, "Anthropic API call"))
                .doOnSuccess(resp -> {
                    if (log.isDebugEnabled())
                        log.debug("[AnthropicAdapter] chatCompletion success provider={} model={}", config.getName(),
                                anthropicRequest.getModel());
                })
                .doOnError(err -> log.debug("[AnthropicAdapter] chatCompletion error provider={} model={} msg={}",
                        config.getName(), anthropicRequest.getModel(), err.toString()));
    }

    @Override
    public Flux<AnthropicStreamEvent> streamChatCompletion(Object request) {
        if (!(request instanceof AnthropicChatRequest anthropicRequest)) {
            return Flux.error(new ProviderException("Invalid request type for AnthropicAdapter: "
                    + (request == null ? "null" : request.getClass().getName())));
        }

        String url = buildMessagesUrl();

        double multiplier = webClientProperties.getTimeout().getStreamReadMultiplier();
        if (multiplier <= 0)
            multiplier = 5.0;
        Duration streamTimeout = Duration
                .ofMillis((long) (webClientProperties.getTimeout().getRead().toMillis() * multiplier));

        if (log.isDebugEnabled()) {
            log.debug("[AnthropicAdapter] streamChatCompletion start provider={} model={} stream=true",
                    config.getName(), anthropicRequest.getModel());
        }

        return webClient.post()
                .uri(url)
                .header(HttpHeaders.ACCEPT, "text/event-stream")
                .bodyValue(anthropicRequest)
                .retrieve()
                .bodyToFlux(String.class)
                .timeout(streamTimeout)
                .onErrorMap(WebClientResponseException.class, this::handleApiError)
                .onErrorMap(ex -> mapToProviderException(ex, "Anthropic streaming API call"))
                .map(this::parseStreamRaw) // 将原始字符串转换为事件对象
                .doOnNext(event -> {
                    if (log.isTraceEnabled()) {
                        log.trace("[AnthropicAdapter] stream event: {}", event);
                    }
                })
                .doOnError(err -> log.debug("[AnthropicAdapter] streamChatCompletion error provider={} model={} msg={}",
                        config.getName(), anthropicRequest.getModel(), err.toString()));
    }


    // ==== 辅助方法 ====

    private String buildMessagesUrl() {
        String base = config.getBaseUrl();
        if (base.endsWith("/"))
            base = base.substring(0, base.length() - 1);
        if (base.endsWith("/v1/messages"))
            return base; // 已经是完整路径
        if (base.endsWith("/v1"))
            return base + "/messages";
        return base + "/v1/messages";
    }

    private List<ModelInfo> getConfiguredModelInfos() {
        // Since we're using database models, this method should be handled by the service layer
        // For now, return empty list as this is not used in the current flow
        return Collections.emptyList();
    }

    private AnthropicStreamEvent parseStreamRaw(String raw) {
        if (raw == null || raw.isEmpty())
            return null;

        if (raw.startsWith(SSE_DATA_PREFIX)) {
            raw = raw.substring(SSE_DATA_PREFIX.length()).trim();

        }

        return JsonUtils.fromJson(raw, AnthropicStreamEvent.class);
    }


    private ProviderException mapToProviderException(Throwable ex, String operation) {
        if (ex instanceof WebClientResponseException webEx) {
            return handleApiError(webEx);
        }
        if (ex instanceof ProviderException pe)
            return pe;
        return new ProviderException(operation + " failed: " + ex.getMessage(), ex);
    }

    private ProviderException handleApiError(WebClientResponseException ex) {
        String body = ex.getResponseBodyAsString();
        int status = ex.getStatusCode().value();
        String errorMessage = String.format("Anthropic API error: %d %s%s", status, ex.getStatusText(),
                body.isEmpty() ? "" : " - " + (body.length() > 200 ? body.substring(0, 200) + "..." : body));
        return switch (status) {
            case 401 -> new ProviderException("Invalid API key or authentication failed", ex);
            case 402 -> new ProviderException("Access forbidden - check permissions", ex);
            case 429 -> {
                if (body.contains("quota"))
                    yield new QuotaExceededException("API quota exceeded", ex);
                else
                    yield new RateLimitException("Rate limit exceeded", ex);
            }
            case 400 ->
                    new ProviderException("Bad request: " + (!body.isEmpty() ? body : "Invalid request parameters"), ex);
            case 404 -> new ProviderException("Endpoint not found", ex);
            case 422 -> new ProviderException(
                    "Unprocessable entity: " + (!body.isEmpty() ? body : "Invalid request format"), ex);
            default -> new ProviderException(errorMessage, ex);
        };
    }

}

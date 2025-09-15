package org.elmo.robella.client.anthropic;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elmo.robella.client.ApiClient;
import org.elmo.robella.common.ProviderType;
import org.elmo.robella.model.entity.Provider;
import org.elmo.robella.config.WebClientProperties;
import org.elmo.robella.exception.ProviderException;
import org.elmo.robella.exception.QuotaExceededException;
import org.elmo.robella.exception.RateLimitException;
import org.elmo.robella.model.anthropic.core.AnthropicChatRequest;
import org.elmo.robella.model.anthropic.core.AnthropicMessage;
import org.elmo.robella.model.anthropic.stream.AnthropicStreamEvent;
import org.elmo.robella.model.internal.UnifiedChatRequest;
import org.elmo.robella.model.internal.UnifiedChatResponse;
import org.elmo.robella.model.internal.UnifiedStreamChunk;
import org.elmo.robella.service.stream.EndpointToUnifiedStreamTransformer;
import org.elmo.robella.service.transform.EndpointTransform;
import org.elmo.robella.service.transform.provider.VendorTransform;
import org.elmo.robella.util.JsonUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

/**
 * Anthropic Messages API 适配器
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Qualifier("ANTHROPIC")
public class AnthropicClient implements ApiClient {

    private static final String SSE_DATA_PREFIX = "data: ";
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    private final WebClient webClient;
    private final WebClientProperties webClientProperties;
    private final EndpointTransform<AnthropicChatRequest, AnthropicMessage> anthropicTransform;
    private final EndpointToUnifiedStreamTransformer<Object> streamTransformer;
    private final Map<ProviderType, VendorTransform<AnthropicChatRequest, AnthropicMessage>> anthropicProviderTransformMap;

    @Override
    public Mono<UnifiedChatResponse> chatCompletion(UnifiedChatRequest request, Provider provider) {
        AnthropicChatRequest anthropicRequest = anthropicTransform.unifiedToEndpointRequest(request);

        // 根据ProviderType调用对应的ProviderTransform
        if (request.getProviderType() != null) {
            VendorTransform<AnthropicChatRequest, AnthropicMessage> providerTransform = anthropicProviderTransformMap.get(request.getProviderType());
            if (providerTransform != null) {
                anthropicRequest = providerTransform.processRequest(anthropicRequest);
            }
        }

        // 为了在 lambda 中使用，创建 final 变量
        final AnthropicChatRequest finalRequest = anthropicRequest;

        String url = buildMessagesUrl(provider);

        if (log.isDebugEnabled()) {
            log.debug("[AnthropicClient] chatCompletion start provider={} model={} stream=false", provider.getName(),
                    finalRequest.getModel());
        }

        return webClient.post()
                .uri(url)
                .headers(headers -> {
                    headers.set("x-api-key", provider.getApiKey());
                    headers.set("anthropic-version", ANTHROPIC_VERSION);
                    headers.set("Content-Type", "application/json");
                })
                .bodyValue(finalRequest)
                .retrieve()
                .bodyToMono(AnthropicMessage.class)
                .map(response -> anthropicTransform.endpointToUnifiedResponse(response))
                .timeout(webClientProperties.getTimeout().getRead())
                .onErrorMap(ex -> mapToProviderException(ex, "Anthropic API call"))
                .doOnSuccess(resp -> {
                    if (log.isDebugEnabled())
                        log.debug("[AnthropicClient] chatCompletion success provider={} model={}", provider.getName(),
                                finalRequest.getModel());
                })
                .doOnError(err -> log.debug("[AnthropicClient] chatCompletion error provider={} model={} msg={}",
                        provider.getName(), finalRequest.getModel(), err.toString()));
    }

    @Override
    public Flux<UnifiedStreamChunk> streamChatCompletion(UnifiedChatRequest request, Provider provider) {
        AnthropicChatRequest anthropicRequest = anthropicTransform.unifiedToEndpointRequest(request);

        // 根据ProviderType调用对应的ProviderTransform
        if (request.getProviderType() != null) {
            VendorTransform<AnthropicChatRequest, AnthropicMessage> providerTransform = anthropicProviderTransformMap.get(request.getProviderType());
            if (providerTransform != null) {
                anthropicRequest = providerTransform.processRequest(anthropicRequest);
            }
        }

        // 为了在 lambda 中使用，创建 final 变量
        final AnthropicChatRequest finalRequest = anthropicRequest;

        String url = buildMessagesUrl(provider);
        String uuid = java.util.UUID.randomUUID().toString();

        double multiplier = webClientProperties.getTimeout().getStreamReadMultiplier();
        if (multiplier <= 0)
            multiplier = 5.0;
        Duration streamTimeout = Duration
                .ofMillis((long) (webClientProperties.getTimeout().getRead().toMillis() * multiplier));

        if (log.isDebugEnabled()) {
            log.debug("[AnthropicClient] streamChatCompletion start provider={} model={} stream=true",
                    provider.getName(), finalRequest.getModel());
        }

        return streamTransformer.transform(webClient.post()
                .uri(url)
                .headers(headers -> {
                    headers.set("x-api-key", provider.getApiKey());
                    headers.set("anthropic-version", ANTHROPIC_VERSION);
                    headers.set(HttpHeaders.ACCEPT, "text/event-stream");
                    headers.set("Content-Type", "application/json");
                })
                .bodyValue(finalRequest)
                .retrieve()
                .bodyToFlux(String.class)
                .timeout(streamTimeout)
                .onErrorMap(WebClientResponseException.class, this::handleApiError)
                .onErrorMap(ex -> mapToProviderException(ex, "Anthropic streaming API call"))
                .map(this::parseStreamRaw)
                .mapNotNull(event -> event)
                .doOnNext(event -> {
                    if (log.isTraceEnabled()) {
                        log.trace("[AnthropicClient] stream event: {}", event);
                    }
                })
                .doOnError(err -> log.debug("[AnthropicClient] streamChatCompletion error provider={} model={} msg={}",
                        provider.getName(), finalRequest.getModel(), err.toString())), uuid);
    }

    private String buildMessagesUrl(Provider provider) {
        String base = provider.getBaseUrl();
        if (base.endsWith("/"))
            base = base.substring(0, base.length() - 1);
        if (base.endsWith("/v1/messages"))
            return base;
        if (base.endsWith("/v1"))
            return base + "/messages";
        return base + "/v1/messages";
    }

    private Object parseStreamRaw(String raw) {
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
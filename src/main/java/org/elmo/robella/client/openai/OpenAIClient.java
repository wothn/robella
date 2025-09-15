package org.elmo.robella.client.openai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elmo.robella.client.ApiClient;
import org.elmo.robella.model.entity.Provider;
import org.elmo.robella.model.internal.UnifiedChatRequest;
import org.elmo.robella.model.internal.UnifiedChatResponse;
import org.elmo.robella.model.internal.UnifiedStreamChunk;
import org.elmo.robella.config.WebClientProperties;
import org.elmo.robella.exception.ProviderException;
import org.elmo.robella.model.openai.core.ChatCompletionRequest;
import org.elmo.robella.model.openai.core.ChatCompletionResponse;
import org.elmo.robella.model.openai.stream.ChatCompletionChunk;
import org.elmo.robella.service.stream.EndpointToUnifiedStreamTransformer;
import org.elmo.robella.service.transform.VendorTransform;
import org.elmo.robella.util.JsonUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * OpenAI API 客户端实现
 * 不再是 Spring Bean，通过 ClientBuilder 创建
 */
@Slf4j
@RequiredArgsConstructor
public class OpenAIClient implements ApiClient {

    private static final String SSE_DONE_MARKER = "[DONE]";

    private final WebClient webClient;
    private final WebClientProperties webClientProperties;
    private final VendorTransform<ChatCompletionRequest, ChatCompletionResponse> openAITransform;
    private final EndpointToUnifiedStreamTransformer<ChatCompletionChunk> streamTransformer;

    @Override
    public Mono<UnifiedChatResponse> chatCompletion(UnifiedChatRequest request, Provider provider) {
        ChatCompletionRequest openaiRequest = openAITransform.unifiedToEndpointRequest(request);

        // 构建URL
        String url = buildChatCompletionsUrl(provider);

        // 发送请求
        if (log.isDebugEnabled()) {
            log.debug("[OpenAIClient] chatCompletion start provider={} model={} stream=false", provider.getName(), openaiRequest.getModel());
            try {
                String requestJson = JsonUtils.toJson(openaiRequest);
                log.debug("[OpenAIClient] chatCompletion request: {}", requestJson);
            } catch (Exception e) {
                log.debug("[OpenAIClient] Failed to serialize request: {}", e.getMessage());
            }
        }

        return webClient.post()
                .uri(url)
                .headers(headers -> {
                    headers.setBearerAuth(provider.getApiKey());
                    headers.set("Content-Type", "application/json");
                })
                .bodyValue(openaiRequest)
                .retrieve()
                .bodyToMono(ChatCompletionResponse.class) 
                .map(response -> openAITransform.endpointToUnifiedResponse(response))
                .timeout(webClientProperties.getTimeout().getRead())
                .onErrorMap(ex -> mapToProviderException(ex, "OpenAI API call"))
                .doOnSuccess(resp -> {
                    if (log.isDebugEnabled())
                        log.debug("[OpenAIClient] chatCompletion success provider={} model={}", provider.getName(), openaiRequest.getModel());
                })
                .doOnError(err -> log.debug("[OpenAIClient] chatCompletion error provider={} model={} msg={}", provider.getName(), openaiRequest.getModel(), err.toString()));
    }

    @Override
    public Flux<UnifiedStreamChunk> streamChatCompletion(UnifiedChatRequest request, Provider provider) {
        ChatCompletionRequest openaiRequest = openAITransform.unifiedToEndpointRequest(request);
        String url = buildChatCompletionsUrl(provider);
        String uuid = java.util.UUID.randomUUID().toString();

        double multiplier = webClientProperties.getTimeout().getStreamReadMultiplier();
        if (multiplier <= 0) multiplier = 5.0;
        Duration streamTimeout = Duration.ofMillis((long) (webClientProperties.getTimeout().getRead().toMillis() * multiplier));

        if (log.isDebugEnabled()) {
            log.debug("[OpenAIClient] streamChatCompletion start provider={} model={} stream=true", provider.getName(), openaiRequest.getModel());
        }

        return streamTransformer.transform(webClient.post()
                .uri(url)
                .headers(headers -> {
                    headers.setBearerAuth(provider.getApiKey());
                    headers.set(HttpHeaders.ACCEPT, "text/event-stream");
                    headers.set("Content-Type", "application/json");
                })
                .bodyValue(openaiRequest)
                .retrieve()
                .bodyToFlux(String.class)
                .mapNotNull(raw -> parseStreamRaw(raw, uuid))
                .timeout(streamTimeout)
                .onErrorMap(ex -> mapToProviderException(ex, "OpenAI streaming API call"))
                .doOnNext(chunk -> {
                    if (log.isTraceEnabled()) {
                        log.trace("[OpenAIClient] stream chunk: id={}, choices={}, usage={}, model={}", 
                            chunk.getId(),
                            chunk.getChoices() != null ? chunk.getChoices().size() : "null",
                            chunk.getUsage() != null ? "present" : "null",
                            chunk.getModel());
                    }
                })
                .doOnError(err -> log.debug("[OpenAIClient] streamChatCompletion error provider={} model={} msg={}", provider.getName(), openaiRequest.getModel(), err.toString())), uuid);
    }

    private ProviderException mapToProviderException(Throwable ex, String operationType) {
        if (ex instanceof WebClientResponseException webEx) {
            String body = webEx.getResponseBodyAsString();
            String errorMessage = String.format("OpenAI API error: %d %s%s",
                    webEx.getStatusCode().value(),
                    webEx.getStatusText(),
                    body);
            return new ProviderException(errorMessage, ex);
        }

        if (ex instanceof ProviderException providerEx) {
            return providerEx;
        }

        return new ProviderException(operationType + " failed: " + ex.getMessage(), ex);
    }

    private String buildChatCompletionsUrl(Provider provider) {
        String baseUrl = provider.getBaseUrl();
        return baseUrl + "/chat/completions";
    }

    private ChatCompletionChunk parseStreamRaw(String raw, String sessionId) {
        if (raw == null || raw.isEmpty()) {
            return null;
        }

        String trimmed = raw.trim();
        if (SSE_DONE_MARKER.equals(trimmed)) {
            log.debug("[OpenAIClient] streamChatCompletion done ");
            return null;
        }

        try {
            ChatCompletionChunk chunk = JsonUtils.fromJson(trimmed, ChatCompletionChunk.class);
            if (chunk != null) {
                return chunk;
            }
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.debug("[OpenAIClient] Failed to parse stream chunk: {}", e.getMessage());
            }
        }

        return null;
    }
}
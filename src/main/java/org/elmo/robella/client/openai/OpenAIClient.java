package org.elmo.robella.client.openai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elmo.robella.client.ApiClient;
import org.elmo.robella.common.ProviderType;
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
import org.elmo.robella.service.transform.EndpointTransform;
import org.elmo.robella.service.transform.provider.VendorTransform;
import org.elmo.robella.util.JsonUtils;
import org.elmo.robella.client.logging.ClientRequestLogger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

/**
 * OpenAI API 客户端实现
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Qualifier("OPENAI")
public class OpenAIClient implements ApiClient {

    private static final String SSE_DONE_MARKER = "[DONE]";

    private final WebClient webClient;
    private final WebClientProperties webClientProperties;
    private final EndpointTransform<ChatCompletionRequest, ChatCompletionResponse> openAIEndpointTransform;
    private final EndpointToUnifiedStreamTransformer<ChatCompletionChunk> streamTransformer;
    private final Map<ProviderType, VendorTransform<ChatCompletionRequest, ChatCompletionResponse>> openaiProviderTransformMap;
    private final ClientRequestLogger clientRequestLogger;

    @Override
    public Mono<UnifiedChatResponse> chatCompletion(UnifiedChatRequest request, Provider provider) {
        String requestId = clientRequestLogger.startRequest();

        ChatCompletionRequest openaiRequest = openAIEndpointTransform.unifiedToEndpointRequest(request);

        // 根据ProviderType调用对应的ProviderTransform
        if (request.getProviderType() != null) {
            VendorTransform<ChatCompletionRequest, ChatCompletionResponse> providerTransform = openaiProviderTransformMap.get(request.getProviderType());
            if (providerTransform != null) {
                openaiRequest = providerTransform.processRequest(openaiRequest);
            }
        }

        // 为了在 lambda 中使用，创建 final 变量
        final ChatCompletionRequest finalRequest = openaiRequest;

        // 构建URL
        String url = buildChatCompletionsUrl(provider);

        // 发送请求
        if (log.isDebugEnabled()) {
            log.debug("[OpenAIClient] chatCompletion start provider={} model={} stream=false", provider.getName(), finalRequest.getModel());
            try {
                String requestJson = JsonUtils.toJson(finalRequest);
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
                .bodyValue(finalRequest)
                .retrieve()
                .bodyToMono(ChatCompletionResponse.class)
                .doOnNext(response -> clientRequestLogger.OpenAIlogSuccess(requestId, finalRequest, response))
                .map(response -> openAIEndpointTransform.endpointToUnifiedResponse(response))
                .timeout(webClientProperties.getTimeout().getRead())
                .onErrorMap(ex -> mapToProviderException(ex, "OpenAI API call"))
                .onErrorResume(error -> clientRequestLogger.OpenAIlogFailure(requestId, finalRequest, error).then(Mono.error(error)))
                .doOnSuccess(resp -> {
                    if (log.isDebugEnabled())
                        log.debug("[OpenAIClient] chatCompletion success provider={} model={}", provider.getName(), finalRequest.getModel());
                })
                .doOnError(err -> log.debug("[OpenAIClient] chatCompletion error provider={} model={} msg={}", provider.getName(), finalRequest.getModel(), err.toString()));
    }

    @Override
    public Flux<UnifiedStreamChunk> streamChatCompletion(UnifiedChatRequest request, Provider provider) {
        String requestId = clientRequestLogger.startRequest();

        ChatCompletionRequest openaiRequest = openAIEndpointTransform.unifiedToEndpointRequest(request);

        // 根据ProviderType调用对应的ProviderTransform
        if (request.getProviderType() != null) {
            VendorTransform<ChatCompletionRequest, ChatCompletionResponse> providerTransform = openaiProviderTransformMap.get(request.getProviderType());
            if (providerTransform != null) {
                openaiRequest = providerTransform.processRequest(openaiRequest);
            }
        }

        // 为了在 lambda 中使用，创建 final 变量
        final ChatCompletionRequest finalRequest = openaiRequest;

        String url = buildChatCompletionsUrl(provider);
        String uuid = UUID.randomUUID().toString();

        double multiplier = webClientProperties.getTimeout().getStreamReadMultiplier();
        if (multiplier <= 0) multiplier = 5.0;
        Duration streamTimeout = Duration.ofMillis((long) (webClientProperties.getTimeout().getRead().toMillis() * multiplier));

        if (log.isDebugEnabled()) {
            log.debug("[OpenAIClient] streamChatCompletion start provider={} model={} stream=true", provider.getName(), finalRequest.getModel());
        }

        clientRequestLogger.startStreamRequest(requestId, request, "openai");

        return streamTransformer.transform(webClient.post()
                .uri(url)
                .headers(headers -> {
                    headers.setBearerAuth(provider.getApiKey());
                    headers.set(HttpHeaders.ACCEPT, "text/event-stream");
                    headers.set("Content-Type", "application/json");
                })
                .bodyValue(finalRequest)
                .retrieve()
                .bodyToFlux(String.class)
                .mapNotNull(raw -> parseStreamRaw(raw, uuid))
                .timeout(streamTimeout)
                .onErrorMap(ex -> mapToProviderException(ex, "OpenAI streaming API call"))
                .doOnNext(chunk -> {
                    clientRequestLogger.logStreamChunk(requestId, chunk);
                    if (log.isTraceEnabled()) {
                        log.trace("[OpenAIClient] stream chunk: id={}, choices={}, usage={}, model={}",
                            chunk.getId(),
                            chunk.getChoices() != null ? chunk.getChoices().size() : "null",
                            chunk.getUsage() != null ? "present" : "null",
                            chunk.getModel());
                    }
                })
                .doOnComplete(() -> clientRequestLogger.completeStreamRequest(requestId, finalRequest, "openai"))
                .doOnError(err -> {
                    clientRequestLogger.failStreamRequest(requestId, finalRequest, "openai", err);
                    log.debug("[OpenAIClient] streamChatCompletion error provider={} model={} msg={}", provider.getName(), finalRequest.getModel(), err.toString());
                }), uuid);
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
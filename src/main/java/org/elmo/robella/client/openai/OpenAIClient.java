package org.elmo.robella.client.openai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elmo.robella.client.ApiClient;
import org.elmo.robella.client.logging.ClientRequestLogger;
import org.elmo.robella.common.ProviderType;
import org.elmo.robella.context.RequestContextHolder;
import org.elmo.robella.model.entity.Provider;
import org.elmo.robella.model.internal.UnifiedChatRequest;
import org.elmo.robella.model.internal.UnifiedChatResponse;
import org.elmo.robella.model.internal.UnifiedStreamChunk;
import org.elmo.robella.exception.ProviderException;
import org.elmo.robella.model.openai.core.ChatCompletionRequest;
import org.elmo.robella.model.openai.core.ChatCompletionResponse;
import org.elmo.robella.model.openai.stream.ChatCompletionChunk;
import org.elmo.robella.service.stream.EndpointToUnifiedStreamTransformer;
import org.elmo.robella.service.transform.EndpointTransform;
import org.elmo.robella.service.transform.provider.VendorTransform;
import org.elmo.robella.util.JsonUtils;
import org.elmo.robella.util.OkHttpUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * OpenAI API 客户端实现
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Qualifier("OPENAI")
public class OpenAIClient implements ApiClient {

    private static final String SSE_DONE_MARKER = "[DONE]";

    private final EndpointTransform<ChatCompletionRequest, ChatCompletionResponse> openAIEndpointTransform;
    private final EndpointToUnifiedStreamTransformer<ChatCompletionChunk> streamTransformer;
    private final Map<ProviderType, VendorTransform<ChatCompletionRequest, ChatCompletionResponse>> openaiProviderTransformMap;
    private final OkHttpUtils okHttpUtils;
    private final ClientRequestLogger clientRequestLogger;
    private final JsonUtils jsonUtils;

    @Override
    public UnifiedChatResponse chat(UnifiedChatRequest request, Provider provider) {
        try {
            // Transform unified request to OpenAI format
            ChatCompletionRequest openaiRequest = openAIEndpointTransform.unifiedToEndpointRequest(request);

            // Apply vendor-specific transformations if needed
            if (request.getProviderType() != null) {
                VendorTransform<ChatCompletionRequest, ChatCompletionResponse> providerTransform = openaiProviderTransformMap
                        .get(request.getProviderType());
                if (providerTransform != null) {
                    openaiRequest = providerTransform.processRequest(openaiRequest);
                }
            }

            // Start logging
            clientRequestLogger.startRequest(openaiRequest, false);

            // Build HTTP headers
            Map<String, String> headers = new ConcurrentHashMap<>();
            headers.put("Authorization", "Bearer " + provider.getApiKey());
            headers.put("Content-Type", "application/json");

            // Make HTTP call
            String url = buildChatCompletionsUrl(provider);
            String responseBody = okHttpUtils.postJson(url, openaiRequest, headers);

            // Parse response
            ChatCompletionResponse response = jsonUtils.fromJson(responseBody, ChatCompletionResponse.class);

            // Transform response bak to unified format
            UnifiedChatResponse unifiedResponse = openAIEndpointTransform.endpointToUnifiedResponse(response);

            // Log success
            clientRequestLogger.completeLog(response, true);

            return unifiedResponse;

        } catch (Exception e) {
            // Log failure
            clientRequestLogger.completeLog(false);
            ProviderException exception = mapToProviderException(e, "OpenAI chat request");
            throw exception;
        }
    }

    @Override
    public Stream<UnifiedStreamChunk> chatStream(UnifiedChatRequest request, Provider provider) {
        try {
            // Transform unified request to OpenAI format
            ChatCompletionRequest openaiRequest = openAIEndpointTransform.unifiedToEndpointRequest(request);

            // Apply vendor-specific transformations if needed
            if (request.getProviderType() != null) {
                VendorTransform<ChatCompletionRequest, ChatCompletionResponse> providerTransform = openaiProviderTransformMap
                        .get(request.getProviderType());
                if (providerTransform != null) {
                    openaiRequest = providerTransform.processRequest(openaiRequest);
                }
            }

            // Start logging
            clientRequestLogger.startRequest(openaiRequest, true);

            // Build HTTP headers for streaming
            Map<String, String> headers = new ConcurrentHashMap<>();
            headers.put("Authorization", "Bearer " + provider.getApiKey());
            headers.put("Content-Type", "application/json");
            headers.put("Accept", "text/event-stream");

            // Make streaming HTTP call
            String url = buildChatCompletionsUrl(provider);
            Stream<String> rawStream = okHttpUtils.postStream(url, openaiRequest, headers);

            // Parse stream chunks first, then transform the entire stream
            Stream<ChatCompletionChunk> parsedStream = rawStream
                    .map(this::parseStreamRaw)
                    .filter(Objects::nonNull)
                    .peek(chunk -> clientRequestLogger.logStreamChunk(chunk));

            // Transform the entire stream at once
            return streamTransformer.transform(parsedStream, RequestContextHolder.getContext().getRequestId())
                    .onClose(() -> {
                        // Stream completed - complete logging
                        clientRequestLogger.completeLog(true);
                    });

        } catch (Exception e) {
            // Log failure
            clientRequestLogger.completeLog(false);
            ProviderException exception = mapToProviderException(e, "OpenAI chat stream request");
            throw exception;
        }
    }

    private ProviderException mapToProviderException(Throwable ex, String operationType) {
        if (ex instanceof ProviderException providerEx) {
            return providerEx;
        }

        return new ProviderException(operationType + " failed: " + ex.getMessage(), ex);
    }

    private String buildChatCompletionsUrl(Provider provider) {
        String baseUrl = provider.getBaseUrl();
        return baseUrl + "/chat/completions";
    }

    private ChatCompletionChunk parseStreamRaw(String raw) {
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.startsWith("data: ")) {
            trimmed = trimmed.substring(5).trim();
        }
        
        if (SSE_DONE_MARKER.equals(trimmed)) {
            log.debug("[OpenAIClient] streamChatCompletion done ");
            return null;
        }

        try {
            ChatCompletionChunk chunk = jsonUtils.fromJson(trimmed, ChatCompletionChunk.class);
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
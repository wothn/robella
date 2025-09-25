package org.elmo.robella.client.anthropic;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elmo.robella.client.ApiClient;
import org.elmo.robella.client.logging.ClientRequestLogger;
import org.elmo.robella.common.ErrorCodeConstants;
import org.elmo.robella.common.ProviderType;
import org.elmo.robella.context.RequestContextHolder;
import org.elmo.robella.model.entity.Provider;
import org.elmo.robella.exception.ApiException;
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
import org.elmo.robella.util.OkHttpUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

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

    private final EndpointTransform<AnthropicChatRequest, AnthropicMessage> anthropicEndpointTransform;
    private final EndpointToUnifiedStreamTransformer<AnthropicStreamEvent> streamTransformer;
    private final Map<ProviderType, VendorTransform<AnthropicChatRequest, AnthropicMessage>> anthropicProviderTransformMap;
    private final OkHttpUtils okHttpUtils;
    private final ClientRequestLogger clientRequestLogger;
    private final JsonUtils jsonUtils;

    @Override
    public UnifiedChatResponse chat(UnifiedChatRequest request, Provider provider) {
        try {
            // Transform unified request to Anthropic format
            AnthropicChatRequest anthropicRequest = anthropicEndpointTransform.unifiedToEndpointRequest(request);

            // Apply vendor-specific transformations if needed
            if (request.getProviderType() != null) {
                VendorTransform<AnthropicChatRequest, AnthropicMessage> providerTransform =
                    anthropicProviderTransformMap.get(request.getProviderType());
                if (providerTransform != null) {
                    anthropicRequest = providerTransform.processRequest(anthropicRequest);
                }
            }

            // Start logging
            clientRequestLogger.startRequest(anthropicRequest, false);

            if (log.isDebugEnabled()) {
                log.debug("[AnthropicClient] chat start provider={} model={} stream=false",
                    provider.getName(), anthropicRequest.getModel());
                try {
                    String requestJson = jsonUtils.toJson(anthropicRequest);
                    log.debug("[AnthropicClient] chat request: {}", requestJson);
                } catch (Exception e) {
                    log.debug("[AnthropicClient] Failed to serialize request: {}", e.getMessage());
                }
            }

            // Build HTTP headers
            Map<String, String> headers = new ConcurrentHashMap<>();
            headers.put("x-api-key", provider.getApiKey());
            headers.put("anthropic-version", ANTHROPIC_VERSION);
            headers.put("Content-Type", "application/json");

            // Make HTTP call
            String url = buildMessagesUrl(provider);
            String responseBody = okHttpUtils.postJson(url, anthropicRequest, headers);

            // Parse response
            AnthropicMessage response = jsonUtils.fromJson(responseBody, AnthropicMessage.class);

            // Transform response back to unified format
            UnifiedChatResponse unifiedResponse = anthropicEndpointTransform.endpointToUnifiedResponse(response);

            // Log success
            clientRequestLogger.completeLog(response, true);

            if (log.isDebugEnabled()) {
                log.debug("[AnthropicClient] chat success provider={} model={}",
                    provider.getName(), anthropicRequest.getModel());
            }

            return unifiedResponse;

        } catch (Exception e) {
            // Log failure
            clientRequestLogger.completeLog(false);
            throw new ApiException(ErrorCodeConstants.PROVIDER_ERROR, "请求出错", e);
        }
    }

    @Override
    public Stream<UnifiedStreamChunk> chatStream(UnifiedChatRequest request, Provider provider) {
        RequestContextHolder.RequestContext ctx = RequestContextHolder.getContext();
        String requestId = ctx.getRequestId();
        try {
            // Transform unified request to Anthropic format
            AnthropicChatRequest anthropicRequest = anthropicEndpointTransform.unifiedToEndpointRequest(request);

            // Apply vendor-specific transformations if needed
            if (request.getProviderType() != null) {
                VendorTransform<AnthropicChatRequest, AnthropicMessage> providerTransform =
                    anthropicProviderTransformMap.get(request.getProviderType());
                if (providerTransform != null) {
                    anthropicRequest = providerTransform.processRequest(anthropicRequest);
                }
            }

            // Start logging
            clientRequestLogger.startRequest(anthropicRequest, true);

            if (log.isDebugEnabled()) {
                log.debug("[AnthropicClient] chatStream start provider={} model={} stream=true",
                    provider.getName(), anthropicRequest.getModel());
            }

            // Build HTTP headers for streaming
            Map<String, String> headers = new ConcurrentHashMap<>();
            headers.put("x-api-key", provider.getApiKey());
            headers.put("anthropic-version", ANTHROPIC_VERSION);
            headers.put("Accept", "text/event-stream");
            headers.put("Content-Type", "application/json");

            // Make streaming HTTP call
            String url = buildMessagesUrl(provider);
            Stream<String> rawStream = okHttpUtils.postStream(url, anthropicRequest, headers);

            // Parse stream chunks first, then transform the entire stream
            Stream<AnthropicStreamEvent> parsedStream = rawStream
                .map(this::parseStreamRaw)
                .filter(Objects::nonNull)
                .peek(event -> {
                    clientRequestLogger.logStreamChunk(event);
                    if (log.isTraceEnabled()) {
                        log.trace("[AnthropicClient] stream event: type={}, content={}",
                            event.getType(), "present");
                    }
                });

            // Transform the entire stream at once
            return streamTransformer.transform(parsedStream, requestId)
                .onClose(() -> {
                    // Stream completed - complete logging
                    clientRequestLogger.completeLog(true);
                });

        } catch (Exception e) {
            // Log failure
            clientRequestLogger.completeLog(false);
            throw new ApiException(ErrorCodeConstants.PROVIDER_ERROR, "请求出错", e);
        }
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

    private AnthropicStreamEvent parseStreamRaw(String raw) {
        if (raw == null || raw.isEmpty())
            return null;

        if (raw.startsWith(SSE_DATA_PREFIX)) {
            raw = raw.substring(SSE_DATA_PREFIX.length()).trim();
        }

        try {
            AnthropicStreamEvent event = jsonUtils.fromJson(raw, AnthropicStreamEvent.class);
            if (event != null) {
                return event;
            }
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.debug("[AnthropicClient] Failed to parse stream chunk: {}", e.getMessage());
            }
        }

        return null;
    }
}
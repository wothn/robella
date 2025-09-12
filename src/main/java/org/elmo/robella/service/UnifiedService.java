package org.elmo.robella.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elmo.robella.model.common.EndpointType;
import org.elmo.robella.model.internal.UnifiedChatRequest;
import org.elmo.robella.model.internal.UnifiedChatResponse;
import org.elmo.robella.model.internal.UnifiedStreamChunk;
import org.elmo.robella.model.openai.core.ChatCompletionRequest;
import org.elmo.robella.model.openai.core.ChatCompletionResponse;
import org.elmo.robella.model.openai.model.ModelListResponse;
import org.elmo.robella.model.openai.model.ModelInfo;
import org.elmo.robella.repository.ModelRepository;
import org.elmo.robella.service.stream.StreamTransformerFactory;
import org.elmo.robella.service.transform.VendorTransformFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UnifiedService {

    private final RoutingService routingService;
    private final StreamTransformerFactory streamTransformerFactory;
    private final ModelRepository modelRepository;
    private final VendorTransformFactory vendorTransformFactory;

    public Mono<ModelListResponse> listModels() {
        return modelRepository.findByPublishedTrue()
            .map(model -> {
                ModelInfo modelInfo = new ModelInfo();
                modelInfo.setId(model.getName());
                modelInfo.setObject("model");
                modelInfo.setOwnedBy(model.getOrganization() != null ? model.getOrganization() : "robella");
                return modelInfo;
            })
            .collectList()
            .map(modelInfos -> {
                ModelListResponse response = new ModelListResponse();
                response.setObject("list");
                response.setData(modelInfos);
                return response;
            });
    }

    // ===== Unified Implementation =====
    public Mono<UnifiedChatResponse> forwardUnified(UnifiedChatRequest request, EndpointType endpointType) {
        String modelName = request.getModel();
        
        // Get the appropriate client and transform
        return routingService.getClientByModel(modelName)
                .flatMap(client -> {
                    // Transform unified request to vendor-specific format
                    Object vendorRequest = vendorTransformFactory.unifiedToVendorRequest(endpointType, request);
                    
                    // Execute the request
                    return client.chatCompletion(vendorRequest)
                            .map(vendorResponse -> vendorTransformFactory.vendorResponseToUnified(endpointType, vendorResponse));
                });
    }

    public Flux<String> streamUnified(UnifiedChatRequest request, EndpointType endpointType) {
        String modelName = request.getModel();
        
        // Generate session ID for stream tracking
        String sessionId = UUID.randomUUID().toString();
        
        // Get the appropriate client
        return routingService.getClientByModel(modelName)
                .flatMapMany(client -> {
                    // Transform unified request to vendor-specific format
                    Object vendorRequest = vendorTransformFactory.unifiedToVendorRequest(endpointType, request);
                    
                    // Execute streaming request
                    return client.streamChatCompletion(vendorRequest);
                })
                .flatMap(vendorChunk -> {
                    // Apply dual-layer stream transformation
                    // Convert vendor chunk to unified format
                    Flux<UnifiedStreamChunk> unifiedChunk = streamTransformerFactory
                            .transformToUnified(endpointType, Flux.just(vendorChunk), sessionId);
                    
                    // Convert unified chunk to endpoint format
                    return streamTransformerFactory
                            .transformToEndpoint(endpointType, unifiedChunk, sessionId);
                })
                .filter(Objects::nonNull);
    }

    // ===== OpenAI Direct Forwarding =====
    
    /**
     * Direct forwarding for OpenAI compatible providers (no conversion needed)
     */
    public Mono<ChatCompletionResponse> forwardOpenAIDirect(ChatCompletionRequest request) {
        String modelName = request.getModel();
        
        // Get the appropriate client and execute directly
        return routingService.getClientByModel(modelName)
                .flatMap(client -> {
                    Mono<?> response = client.chatCompletion(request);
                    return response.cast(ChatCompletionResponse.class);
                });
    }

    /**
     * 支持 OpenAI 兼容提供商的直接流式传输（无需转换）
     * 注意：这将返回原始流数据 - 对于 OpenAI 端点，您可能需要 
     * 额外的转换才能将 ChatCompletionChunk 转换为 String 格式
     */
    public Flux<String> streamOpenAIDirect(ChatCompletionRequest request) {
        String modelName = request.getModel();
        
        // Get the appropriate client and execute streaming directly
        return routingService.getClientByModel(modelName)
                .flatMapMany(client -> {
                    Flux<?> stream = client.streamChatCompletion(request);
                    // For OpenAI clients, the stream returns ChatCompletionChunk objects
                    // We need to convert them to SSE format strings
                    return stream.cast(org.elmo.robella.model.openai.stream.ChatCompletionChunk.class)
                            .map(chunk -> {
                                try {
                                    return "data: " + org.elmo.robella.util.JsonUtils.toJson(chunk) + "\n\n";
                                } catch (Exception e) {
                                    log.error("Failed to serialize chunk to JSON", e);
                                    return null;
                                }
                            })
                            .filter(java.util.Objects::nonNull)
                            .concatWith(Flux.just("data: [DONE]\n\n"));
                });
    }

}
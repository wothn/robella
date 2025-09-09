package org.elmo.robella.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elmo.robella.model.common.EndpointType;
import org.elmo.robella.model.internal.UnifiedChatRequest;
import org.elmo.robella.model.internal.UnifiedChatResponse;
import org.elmo.robella.model.internal.UnifiedStreamChunk;
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
public class ForwardingService {

    private final RoutingService routingService;
    private final StreamTransformerFactory streamTransformerFactory;
    private final ModelRepository modelRepository;
    private final VendorTransformFactory vendorTransformFactory;

    public Mono<ModelListResponse> listModels() {
        return modelRepository.findByIsPublishedTrue()
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
        String vendorModelName = mapModelName(modelName);
        
        // Update request with vendor model name
        request.setModel(vendorModelName);
        
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
        String vendorModelName = mapModelName(modelName);
        
        // Update request with vendor model name
        request.setModel(vendorModelName);
        
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

    /**
     * Map client model name to vendor model name
     */
    private String mapModelName(String clientModelName) {
        // For now, return the same name - implement model mapping logic as needed
        return clientModelName;
    }

}
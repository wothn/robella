package org.elmo.robella.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elmo.robella.model.openai.ModelListResponse;
import org.elmo.robella.model.internal.UnifiedChatRequest;
import org.elmo.robella.model.internal.UnifiedChatResponse;
import org.elmo.robella.model.internal.UnifiedStreamChunk;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class ForwardingServiceImpl implements ForwardingService {

    private final TransformService transformService;
    private final RoutingService routingService;

    @Override
    public Mono<ModelListResponse> listModels() {
        // 委托给 RoutingService 处理模型列表
        ModelListResponse models = routingService.getAvailableModels();
        return Mono.just(models);
    }

    @Override
    public void refreshModelCache() {
        // 委托给 RoutingService 处理缓存刷新
        log.debug("Delegating model cache refresh to RoutingService");
        routingService.refreshModelCache();
    }

    // ===== Unified Implementation =====
    @Override
    public Mono<UnifiedChatResponse> forwardUnified(UnifiedChatRequest request, String forcedProvider) {
        String providerName = forcedProvider != null ? forcedProvider : routingService.decideProviderByModel(request.getModel());
        var adapter = routingService.getAdapter(providerName);
        Object vendorReq = transformService.unifiedToVendorRequest(request, providerName);
        return adapter.chatCompletion(vendorReq)
                .map(resp -> transformService.vendorResponseToUnified(resp, providerName));
    }

    @Override
    public Flux<UnifiedStreamChunk> streamUnified(UnifiedChatRequest request, String forcedProvider) {
        // 是否指定厂商
        String providerName = forcedProvider != null ? forcedProvider : routingService.decideProviderByModel(request.getModel());
        var adapter = routingService.getAdapter(providerName);
        Object vendorReq = transformService.unifiedToVendorRequest(request, providerName);
        return adapter.streamChatCompletion(vendorReq)
                .map(event -> transformService.vendorStreamEventToUnified(event, providerName))
                .filter(ch -> ch != null && (ch.getContentDelta() != null || ch.isFinished()));
    }

}

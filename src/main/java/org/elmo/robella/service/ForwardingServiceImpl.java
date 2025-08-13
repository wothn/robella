package org.elmo.robella.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elmo.robella.model.openai.ModelListResponse;
import org.elmo.robella.model.internal.UnifiedChatRequest;
import org.elmo.robella.model.internal.UnifiedChatResponse;
import org.elmo.robella.model.internal.UnifiedStreamChunk;
import org.springframework.stereotype.Service;
import org.elmo.robella.util.ConfigUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class ForwardingServiceImpl implements ForwardingService {

    private final TransformService transformService;
    private final RoutingService routingService;
    private final ConfigUtils configUtils; // 用于逻辑模型 -> vendor模型映射

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
        // 映射逻辑模型名为厂商真实模型名
        UnifiedChatRequest effective = mapToVendorModel(request, providerName);
        Object vendorReq = transformService.unifiedToVendorRequest(effective, providerName);
        return adapter.chatCompletion(vendorReq)
                .map(resp -> transformService.vendorResponseToUnified(resp, providerName));
    }

    @Override
    public Flux<UnifiedStreamChunk> streamUnified(UnifiedChatRequest request, String forcedProvider) {
        // 决定提供者
        String providerName = forcedProvider != null ? forcedProvider : routingService.decideProviderByModel(request.getModel());
        // 获取适配器
        var adapter = routingService.getAdapter(providerName);
        // 获取实际模型名，并确保流式标志为true
        UnifiedChatRequest effective = mapToVendorModel(request, providerName);
        if (effective.getStream() == null || !effective.getStream()) {
            effective = effective.toBuilder().stream(true).build();
            if (log.isTraceEnabled()) {
                log.trace("Force enabled stream=true for unified request");
            }
        }
        // 转换为适配器特定格式
        Object vendorReq = transformService.unifiedToVendorRequest(effective, providerName);
        return adapter.streamChatCompletion(vendorReq)
                .map(event -> transformService.vendorStreamEventToUnified(event, providerName))
                .doOnNext(ch -> {
                    if (ch == null) {
                        if (log.isTraceEnabled())
                            log.trace("Stream chunk null after transform (provider={})", providerName);
                    } else if (log.isTraceEnabled()) {
                        log.trace("Stream chunk before filter: finished={} contentDelta='{}' reasoningDelta='{}' toolCalls={} usagePresent={} hasPayload={}",
                                ch.isFinished(),
                                truncate(ch.getContentDelta()),
                                truncate(ch.getReasoningDelta()),
                                ch.getToolCallDeltas() == null ? 0 : ch.getToolCallDeltas().size(),
                                ch.getUsage() != null,
                                ch.hasPayload());
                    }
                })
                .filter(ch -> ch != null && ch.hasPayload())
                .doOnComplete(() -> {
                    if (log.isDebugEnabled())
                        log.debug("Streaming unified response completed: provider={}", providerName);
                });
    }

    // ---- helpers ----
    private UnifiedChatRequest mapToVendorModel(UnifiedChatRequest original, String providerName) {
        if (original == null || original.getModel() == null) return original;
        try {
            String vendorModel = configUtils.getModelMapping(providerName, original.getModel());
            if (vendorModel != null && !vendorModel.isEmpty() && !vendorModel.equals(original.getModel())) {
                return original.toBuilder().model(vendorModel).build();
            }
            return original;
        } catch (Exception e) {
            // 安全降级：出现异常则返回原请求
            log.debug("model mapping failed for provider {} model {}: {}", providerName, original.getModel(), e.getMessage());
            return original;
        }
    }

    // --- local private utility (not part of public API) ---
    private static String truncate(String s) {
        if (s == null) return null;
        if (s.length() <= 100) return s;
        return s.substring(0, 100) + "...";
    }

}

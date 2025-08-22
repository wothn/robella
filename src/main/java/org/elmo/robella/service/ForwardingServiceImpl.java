package org.elmo.robella.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elmo.robella.adapter.AIProviderAdapter;
import org.elmo.robella.model.internal.UnifiedChatRequest;
import org.elmo.robella.model.internal.UnifiedChatResponse;
import org.elmo.robella.model.internal.UnifiedStreamChunk;
import org.elmo.robella.model.openai.model.ModelListResponse;
import org.springframework.stereotype.Service;
import org.elmo.robella.util.ConfigUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Objects;

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
        // 路由供应商
        String providerName = forcedProvider != null ? forcedProvider : routingService.decideProviderByModel(request.getModel());
        // 获取适配器
        AIProviderAdapter adapter = routingService.getAdapter(providerName);
        // 映射逻辑模型名为厂商真实模型名
        UnifiedChatRequest effective = mapToVendorModel(request, providerName);
        // 获取thinkingField配置
        String thinkingField = configUtils.getThinkingField(providerName, request.getModel());
        // 转换为供应商所需格式（传递thinkingField配置）
        Object vendorReq = transformService.unifiedToVendorRequest(effective, providerName, thinkingField);
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
            // 为流式调用设置 stream=true，直接修改请求以避免克隆（允许原始请求被修改以减少对象分配）
            effective.setStream(true);
        }
        // 获取thinkingField配置
        String thinkingField = configUtils.getThinkingField(providerName, request.getModel());
        // 转换为适配器特定格式（传递thinkingField配置）
        Object vendorReq = transformService.unifiedToVendorRequest(effective, providerName, thinkingField);
        return adapter.streamChatCompletion(vendorReq)
                .map(event -> transformService.vendorStreamEventToUnified(event, providerName))
                .filter(Objects::nonNull) // 只过滤掉转换后为null的事件
                .doOnComplete(() -> {
                    if (log.isDebugEnabled())
                        log.debug("Streaming unified response completed: provider={}", providerName);
                });
    }

    // ---- 私有辅助方法 ----

    private UnifiedChatRequest mapToVendorModel(UnifiedChatRequest original, String providerName) {
        if (original == null || original.getModel() == null) return original;
        try {
            String vendorModel = configUtils.getModelMapping(providerName, original.getModel());
            if (vendorModel != null && !vendorModel.isEmpty() && !vendorModel.equals(original.getModel())) {
                // 直接在原始请求上设置 vendor 模型名，避免创建副本以减少对象分配
                original.setModel(vendorModel);
            }
            return original;
        } catch (Exception e) {
            // 安全降级：出现异常则返回原请求
            log.debug("model mapping failed for provider {} model {}: {}", providerName, original.getModel(), e.getMessage());
            return original;
        }
    }

}

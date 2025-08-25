package org.elmo.robella.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elmo.robella.adapter.AIProviderAdapter;
import org.elmo.robella.model.internal.UnifiedChatRequest;
import org.elmo.robella.model.internal.UnifiedChatResponse;
import org.elmo.robella.model.internal.UnifiedStreamChunk;
import org.elmo.robella.model.openai.model.ModelListResponse;
import org.elmo.robella.util.ConfigUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class ForwardingServiceImpl implements ForwardingService {

    private final TransformService transformService;
    private final RoutingService routingService;
    private final ConfigUtils configUtils;

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

    /**
     * 处理模型名称映射，将客户端请求的模型名称映射到供应商实际的模型名称
     */
    private String mapModelName(String providerName, String originalModel) {
        String vendorModel = configUtils.getModelMapping(providerName, originalModel);
        if (vendorModel != null && !vendorModel.isEmpty() && !vendorModel.equals(originalModel)) {
            log.debug("Model mapped from '{}' to '{}' for provider '{}'", originalModel, vendorModel, providerName);
            return vendorModel;
        }
        return originalModel;
    }

    // ===== Unified Implementation =====
    @Override
    public Mono<UnifiedChatResponse> forwardUnified(UnifiedChatRequest request, String forcedProvider) {
        // 路由供应商（使用原始模型名称进行路由决策）
        String providerName = forcedProvider != null ? forcedProvider : routingService.decideProviderByModel(request.getModel());

        // 获取适配器
        AIProviderAdapter adapter = routingService.getAdapter(providerName);

        // 在调用转换服务之前处理模型映射
        String originalModel = request.getModel();
        String vendorModel = mapModelName(providerName, originalModel);
        if (!vendorModel.equals(originalModel)) {
            request.setModel(vendorModel);
        }

        // 转换为供应商所需格式
        Object vendorReq = transformService.unifiedToVendorRequest(request, providerName);
        return adapter.chatCompletion(vendorReq)
                .map(resp -> transformService.vendorResponseToUnified(resp, providerName));
    }

    @Override
    public Flux<UnifiedStreamChunk> streamUnified(UnifiedChatRequest request, String forcedProvider) {
        // 决定提供者
        String providerName = forcedProvider != null ? forcedProvider : routingService.decideProviderByModel(request.getModel());
        // 获取适配器
        AIProviderAdapter adapter = routingService.getAdapter(providerName);

        // 在调用转换服务之前处理模型映射
        String originalModel = request.getModel();
        String vendorModel = mapModelName(providerName, originalModel);
        if (!vendorModel.equals(originalModel)) {
            request.setModel(vendorModel);
        }

        // 转换为适配器特定格式
        Object vendorReq = transformService.unifiedToVendorRequest(request, providerName);
        return adapter.streamChatCompletion(vendorReq)
                .map(event -> transformService.vendorStreamEventToUnified(event, providerName))
                .filter(Objects::nonNull) // 只过滤掉转换后为null的事件
                .doOnComplete(() -> {
                    if (log.isDebugEnabled())
                        log.debug("Streaming unified response completed: provider={}", providerName);
                });
    }

}

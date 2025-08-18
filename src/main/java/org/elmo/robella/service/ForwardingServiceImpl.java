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
            // 创建一个新的请求对象，复制所有字段并设置 stream 为 true
            UnifiedChatRequest streamRequest = copyUnifiedChatRequest(effective);
            streamRequest.setStream(true);
            effective = streamRequest;
        }
        // 转换为适配器特定格式
        Object vendorReq = transformService.unifiedToVendorRequest(effective, providerName);
        return adapter.streamChatCompletion(vendorReq)
                .map(event -> transformService.vendorStreamEventToUnified(event, providerName))
                .filter(Objects::nonNull) // 过滤掉转换后为null的事件
                .doOnComplete(() -> {
                    if (log.isDebugEnabled())
                        log.debug("Streaming unified response completed: provider={}", providerName);
                });
    }

    // ---- helpers ----
    
    /**
     * 深度复制 UnifiedChatRequest 对象
     */
    private UnifiedChatRequest copyUnifiedChatRequest(UnifiedChatRequest original) {
        if (original == null) {
            return null;
        }
        
        UnifiedChatRequest copy = new UnifiedChatRequest();
        copy.setModel(original.getModel());
        copy.setMessages(original.getMessages());
        copy.setMaxTokens(original.getMaxTokens());
        copy.setTemperature(original.getTemperature());
        copy.setTopP(original.getTopP());
        copy.setTopK(original.getTopK());
        copy.setStop(original.getStop());
        copy.setStream(original.getStream());
        copy.setStreamOptions(original.getStreamOptions());
        copy.setTools(original.getTools());
        copy.setToolChoice(original.getToolChoice());
        copy.setFrequencyPenalty(original.getFrequencyPenalty());
        copy.setPresencePenalty(original.getPresencePenalty());
        copy.setLogprobs(original.getLogprobs());
        copy.setTopLogprobs(original.getTopLogprobs());
        copy.setN(original.getN());
        copy.setResponseFormat(original.getResponseFormat());
        copy.setParallelToolCalls(original.getParallelToolCalls());
        copy.setModalities(original.getModalities());
        copy.setPrediction(original.getPrediction());
        copy.setAudio(original.getAudio());
        copy.setPromptCacheKey(original.getPromptCacheKey());
        copy.setTextOptions(original.getTextOptions());
        copy.setThinkingOptions(original.getThinkingOptions());
        copy.setVendorExtras(original.getVendorExtras());
        copy.setUndefined(original.getUndefined());
        copy.setTempFields(original.getTempFields());
        
        return copy;
    }
    
    private UnifiedChatRequest mapToVendorModel(UnifiedChatRequest original, String providerName) {
        if (original == null || original.getModel() == null) return original;
        try {
            String vendorModel = configUtils.getModelMapping(providerName, original.getModel());
            if (vendorModel != null && !vendorModel.isEmpty() && !vendorModel.equals(original.getModel())) {
                // 创建一个新的请求对象，复制所有字段并设置新的模型
                UnifiedChatRequest newRequest = copyUnifiedChatRequest(original);
                newRequest.setModel(vendorModel);
                return newRequest;
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

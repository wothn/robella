package org.elmo.robella.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elmo.robella.client.ApiClient;
import org.elmo.robella.config.ProviderType;
import org.elmo.robella.model.internal.UnifiedChatRequest;
import org.elmo.robella.model.internal.UnifiedChatResponse;
import org.elmo.robella.model.internal.UnifiedStreamChunk;
import org.elmo.robella.model.openai.model.ModelListResponse;
import org.elmo.robella.service.stream.StreamTransformerFactory;
import org.elmo.robella.service.stream.StreamToUnifiedTransformer;
import org.elmo.robella.service.stream.UnifiedToEndpointTransformer;
import org.elmo.robella.service.transform.TransformService;
import org.elmo.robella.util.ConfigUtils;
import org.elmo.robella.util.JsonUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ForwardingServiceImpl implements ForwardingService {

    private final TransformService transformService;
    private final RoutingService routingService;
    private final ConfigUtils configUtils;
    private final StreamTransformerFactory streamTransformerFactory;

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
            return vendorModel;
        }
        return originalModel;
    }

    // ===== Unified Implementation =====
    @Override
    public Mono<UnifiedChatResponse> forwardUnified(UnifiedChatRequest request, String providerName) {
        // 路由供应商（使用原始模型名称进行路由决策）
        String providerNameUsed = providerName != null ? providerName : routingService.decideProviderByModel(request.getModel());

        // 获取适配器
        ApiClient client = routingService.getClient(providerNameUsed);

        // 模型名称映射
        request.setModel(mapModelName(providerNameUsed, request.getModel()));

        // 转换为供应商所需格式
        Object vendorReq = transformService.unifiedToVendorRequest(request, providerNameUsed);
        return client.chatCompletion(vendorReq)
                .map(resp -> transformService.vendorResponseToUnified(resp, providerNameUsed));
    }

    @Override
    public Flux<String> streamUnified(UnifiedChatRequest request, String providerName, String endpointFormat) {
        // 决定提供者
        String providerNameUsed = providerName != null ? providerName : routingService.decideProviderByModel(request.getModel());
        // 获取适配器
        ApiClient client = routingService.getClient(providerNameUsed);

        // 在调用转换服务之前处理模型映射
        request.setModel(mapModelName(providerNameUsed, request.getModel()));

        // 转换为供应商所需格式
        Object vendorReq = transformService.unifiedToVendorRequest(request, providerNameUsed);

        // 获取后端提供商类型
        String providerTypeStr = routingService.getProviderType(providerNameUsed);
        ProviderType providerType = ProviderType.fromString(providerTypeStr);
        
        // 获取端点格式类型（从endpointFormat参数转换）
        ProviderType endpointProviderType = ProviderType.fromString(endpointFormat);
        
        // 生成会话ID用于流式转换状态管理
        String sessionId = UUID.randomUUID().toString();
        
        // 获取供应商特定的流式响应
        Flux<?> vendorStream = client.streamChatCompletion(vendorReq);
        
        // 将供应商流式响应转换为统一格式
        StreamToUnifiedTransformer streamToUnifiedTransformer = streamTransformerFactory.getStreamToUnifiedTransformer(providerType);
        Flux<UnifiedStreamChunk> unifiedStream = streamToUnifiedTransformer.transformToUnified(vendorStream, sessionId);
        
        // 将统一格式转换为端点格式（使用端点格式类型）
        UnifiedToEndpointTransformer unifiedToEndpointTransformer = streamTransformerFactory.getUnifiedToEndpointTransformer(endpointProviderType);
        
        // 转换为Flux<String>以适配Controller的返回类型
        Flux<?> endpointStream = unifiedToEndpointTransformer.transformToEndpoint(unifiedStream, sessionId);
        return endpointStream.mapNotNull(obj -> {
            if (obj instanceof String) {
                return (String) obj;
            } else if (obj != null) {
                // 对于非字符串对象，转换为JSON字符串
                return JsonUtils.toJson(obj);
            }
            return null; // 这里应该被filter过滤掉
        }).filter(Objects::nonNull);
    }

}
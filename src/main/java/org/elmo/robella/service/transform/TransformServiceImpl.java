package org.elmo.robella.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elmo.robella.config.ProviderConfig;
import org.elmo.robella.model.internal.UnifiedChatRequest;
import org.elmo.robella.model.internal.UnifiedChatResponse;
import org.elmo.robella.model.internal.UnifiedStreamChunk;
import org.elmo.robella.service.endpoint.EndpointFamily;
import org.elmo.robella.service.endpoint.StreamTranscodingService;
import org.elmo.robella.service.transform.VendorTransformRegistry;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;


@Slf4j
@Service
@RequiredArgsConstructor
public class TransformServiceImpl implements TransformService {

    private final ProviderConfig providerConfig;
    private final VendorTransformRegistry registry;
    private final StreamTranscodingService streamTranscodingService;

    @Override
    public VendorTransform getVendorTransform(String providerType) {
        return registry.get(providerType == null ? "OpenAI" : providerType);
    }
    
    private String providerTypeByName(String providerName) {
        if (providerName == null) return "OpenAI";
        var p = providerConfig.getProviders().get(providerName);
        return p != null ? p.getType() : "OpenAI";
    }
    
    
    // ===== 通用 =====
    @Override
    public Object unifiedToVendorRequest(UnifiedChatRequest unified, String providerName) {
        String providerType = providerTypeByName(providerName);
        
        // 设置 providerName 到 unified 对象中
        unified.setProviderName(providerName);
        
        // 调用具体的转换器（模型映射已在ForwardingService中处理）
        return getVendorTransform(providerType).unifiedToVendorRequest(unified);
    }

    @Override
    public UnifiedChatResponse vendorResponseToUnified(Object vendorResp, String providerName) {
        String providerType = providerTypeByName(providerName);
        return getVendorTransform(providerType).vendorResponseToUnified(vendorResp);
    }

    @Override
    public UnifiedStreamChunk vendorStreamEventToUnified(Object vendorEvent, String providerName) {
        String providerType = providerTypeByName(providerName);
        return getVendorTransform(providerType).vendorStreamEventToUnified(vendorEvent);
    }
    
    // ===== 新增接口方法以支持端点格式与提供商分离 =====
    
    /**
     * 根据端点格式转换请求到统一格式（与实际调用的provider无关）
     */
    @Override
    public UnifiedChatRequest endpointRequestToUnified(Object vendorRequest, String endpointType) {
        return getVendorTransform(endpointType).vendorRequestToUnified(vendorRequest);
    }
    
    /**
     * 根据端点格式从统一格式转换响应（与实际调用的provider无关）
     */
    @Override
    public Object unifiedToEndpointResponse(UnifiedChatResponse unifiedResponse, String endpointType) {
        return getVendorTransform(endpointType).unifiedToVendorResponse(unifiedResponse);
    }
    
    /**
     * 根据端点格式从统一流片段转换为端点格式（与实际调用的provider无关）
     */
    @Override
    public String unifiedStreamChunkToEndpoint(UnifiedStreamChunk chunk, String endpointType) {
        return getVendorTransform(endpointType).unifiedStreamChunkToVendor(chunk);
    }
    
    // ===== 新增：有状态流式转换 =====
    
    /**
     * 有状态流式转换：支持端点族间的协议转换
     */
    @Override
    public Flux<String> transcodeStreamWithFamily(EndpointFamily sourceFamily, EndpointFamily targetFamily, Flux<UnifiedStreamChunk> chunkFlux) {
        return streamTranscodingService.transcodeStream(sourceFamily, targetFamily, chunkFlux);
    }
}

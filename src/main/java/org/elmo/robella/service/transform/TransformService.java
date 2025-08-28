package org.elmo.robella.service;

import org.elmo.robella.model.internal.UnifiedChatRequest;
import org.elmo.robella.model.internal.UnifiedChatResponse;
import org.elmo.robella.model.internal.UnifiedStreamChunk;
import org.elmo.robella.service.endpoint.EndpointFamily;
import reactor.core.publisher.Flux;

/**
 * 统一转换入口：内部通过厂商类型 (providerType) 分发到对应的 VendorTransform 实现。
 * providerName -> providerType 的映射来源于 ProviderConfig。
 */
public interface TransformService {

    // 获取具体厂商转换器（按 providerType，例如 OpenAI / Anthropic / Gemini 等）
    VendorTransform getVendorTransform(String providerType);

    // ========== 通用：供 ForwardingService 调用 ==========
    Object unifiedToVendorRequest(UnifiedChatRequest unified, String providerName);
    UnifiedChatResponse vendorResponseToUnified(Object vendorResp, String providerName);
    UnifiedStreamChunk vendorStreamEventToUnified(Object vendorEvent, String providerName);
    
    // ========== 端点格式转换：支持端点格式与提供商分离 ==========
    /**
     * 根据端点格式转换请求到统一格式（与实际调用的provider无关）
     */
    UnifiedChatRequest endpointRequestToUnified(Object vendorRequest, String endpointType);
    
    /**
     * 根据端点格式从统一格式转换响应（与实际调用的provider无关）
     */
    Object unifiedToEndpointResponse(UnifiedChatResponse unifiedResponse, String endpointType);
    
    /**
     * 根据端点格式从统一流片段转换为端点格式（与实际调用的provider无关）
     */
    String unifiedStreamChunkToEndpoint(UnifiedStreamChunk chunk, String endpointType);
    
    // ========== 新增：有状态流式转换 ==========
    /**
     * 有状态流式转换：支持端点族间的协议转换
     * 
     * @param sourceFamily 源端点族（入口）
     * @param targetFamily 目标端点族（输出）
     * @param chunkFlux 输入的UnifiedStreamChunk流
     * @return 目标格式的SSE事件字符串流
     */
    Flux<String> transcodeStreamWithFamily(EndpointFamily sourceFamily, EndpointFamily targetFamily, Flux<UnifiedStreamChunk> chunkFlux);
}
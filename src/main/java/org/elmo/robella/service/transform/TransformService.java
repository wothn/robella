package org.elmo.robella.service.transform;

import org.elmo.robella.model.internal.UnifiedChatRequest;
import org.elmo.robella.model.internal.UnifiedChatResponse;

/**
 * 统一转换入口：内部通过厂商类型 (providerType) 分发到对应的 VendorTransform 实现。
 * providerName -> providerType 的映射来源于 ProviderConfig。
 */
public interface TransformService {

    // 获取具体厂商转换器（按 providerType，例如 OpenAI / Anthropic / Gemini 等）
    VendorTransform getVendorTransform(String providerType);

    UnifiedChatResponse vendorResponseToUnified(Object vendorResp, String providerName);
    Object unifiedToVendorRequest(UnifiedChatRequest unified, String providerName);


    /**
     * 根据端点格式转换请求到统一格式（与实际调用的provider无关）
     */
    UnifiedChatRequest endpointRequestToUnified(Object vendorRequest, String endpointType);
    
    /**
     * 根据端点格式从统一格式转换响应（与实际调用的provider无关）
     */
    Object unifiedToEndpointResponse(UnifiedChatResponse unifiedResponse, String endpointType);


}
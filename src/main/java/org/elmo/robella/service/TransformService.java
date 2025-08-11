package org.elmo.robella.service;

import org.elmo.robella.model.internal.UnifiedChatRequest;
import org.elmo.robella.model.internal.UnifiedChatResponse;
import org.elmo.robella.model.internal.UnifiedStreamChunk;

/**
 * 统一转换入口：内部通过厂商类型 (providerType) 分发到对应的 VendorTransform 实现。
 * providerName -> providerType 的映射来源于 ProviderConfig。
 */
public interface TransformService {

    // 获取具体厂商转换器（按 providerType，例如 OpenAI / Anthropic / Gemini 等）
    VendorTransform getVendorTransform(String providerType);

    // ========== 通用：供 ForwardingService 调用 ==========
    Object unifiedToVendorRequest(UnifiedChatRequest unified, String providerName);
    UnifiedChatRequest vendorRequestToUnified(Object vendorRequest, String providerName);
    UnifiedChatResponse vendorResponseToUnified(Object vendorResp, String providerName);
    Object unifiedToVendorResponse(UnifiedChatResponse unifiedResponse, String providerName);
    UnifiedStreamChunk vendorStreamEventToUnified(Object vendorEvent, String providerName);
    Object unifiedStreamChunkToVendor(UnifiedStreamChunk chunk, String providerName);


}
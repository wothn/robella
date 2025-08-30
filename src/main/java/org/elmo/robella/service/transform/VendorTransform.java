package org.elmo.robella.service.transform;

import org.elmo.robella.model.internal.UnifiedChatRequest;
import org.elmo.robella.model.internal.UnifiedChatResponse;
import org.elmo.robella.model.internal.UnifiedStreamChunk;

/**
 * 单一厂商类型转换器。每个厂商需提供四个方向的转换，不处理流式转换：
 * 1. 厂商请求 -> Unified
 * 2. Unified -> 厂商请求
 * 3. 厂商响应 -> Unified
 * 4. Unified -> 厂商响应
 *
 */
public interface VendorTransform {
    /** 返回该转换器支持的 ProviderType 名称（枚举 name）。 */
    String type();

    // 厂商请求 -> Unified
    UnifiedChatRequest vendorRequestToUnified(Object vendorRequest);
    // Unified -> 厂商请求
    Object unifiedToVendorRequest(UnifiedChatRequest unifiedRequest);
    // 厂商响应 -> Unified
    UnifiedChatResponse vendorResponseToUnified(Object vendorResponse);
    // Unified -> 厂商响应
    Object unifiedToVendorResponse(UnifiedChatResponse unifiedResponse);
}

package org.elmo.robella.service;

import org.elmo.robella.model.internal.UnifiedChatRequest;
import org.elmo.robella.model.internal.UnifiedChatResponse;
import org.elmo.robella.model.internal.UnifiedStreamChunk;

/**
 * 单一厂商类型转换器。每个厂商需提供六个方向的转换：
 * 1. 厂商请求 -> Unified
 * 2. Unified -> 厂商请求
 * 3. 厂商响应 -> Unified
 * 4. Unified -> 厂商响应
 * 5. 厂商流事件 -> Unified 流片段
 * 6. Unified 流片段 -> 端点流事件
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
    // 厂商流事件 -> Unified
    UnifiedStreamChunk vendorStreamEventToUnified(Object vendorEvent);
    // Unified -> 厂商流事件
    String unifiedStreamChunkToVendor(UnifiedStreamChunk chunk);
}

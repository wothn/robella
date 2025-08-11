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
 * 6. Unified 流片段 -> 厂商流事件
 *
 * 对于暂不支持的方向，可返回 null 或抛出 UnsupportedOperationException。
 */
public interface VendorTransform {
    /** 返回该转换器支持的 ProviderType 名称（枚举 name）。 */
    String type();

    // 1
    UnifiedChatRequest vendorRequestToUnified(Object vendorRequest);
    // 2
    Object unifiedToVendorRequest(UnifiedChatRequest unifiedRequest);
    // 3
    UnifiedChatResponse vendorResponseToUnified(Object vendorResponse);
    // 4
    Object unifiedToVendorResponse(UnifiedChatResponse unifiedResponse);
    // 5
    UnifiedStreamChunk vendorStreamEventToUnified(Object vendorEvent);
    // 6
    Object unifiedStreamChunkToVendor(UnifiedStreamChunk chunk);
}

package org.elmo.robella.service.transform;

import org.elmo.robella.model.common.EndpointType;
import org.elmo.robella.model.internal.UnifiedChatRequest;
import org.elmo.robella.model.internal.UnifiedChatResponse;

/**
 * 单一厂商类型转换器。每个厂商需提供四个方向的转换，不处理流式转换：
 * 1. 厂商请求 -> Unified
 * 2. Unified -> 厂商请求
 * 3. 厂商响应 -> Unified
 * 4. Unified -> 厂商响应
 *
 * @param <V> 厂商请求类型
 * @param <R> 厂商响应类型
 */
public interface VendorTransform<V, R> {
    /** 返回该转换器支持的 ProviderType 名称（枚举 name）。 */
    EndpointType type();

    // 厂商请求 -> Unified
    UnifiedChatRequest vendorRequestToUnified(V vendorRequest);
    // Unified -> 厂商请求
    V unifiedToVendorRequest(UnifiedChatRequest unifiedRequest);
    // 厂商响应 -> Unified
    UnifiedChatResponse vendorResponseToUnified(R vendorResponse);
    // Unified -> 厂商响应
    R unifiedToVendorResponse(UnifiedChatResponse unifiedResponse);
}

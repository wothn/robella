package org.elmo.robella.service.transform;

import org.elmo.robella.model.common.EndpointType;
import org.elmo.robella.model.internal.UnifiedChatRequest;
import org.elmo.robella.model.internal.UnifiedChatResponse;

/**
 * 单一厂商类型转换器。每个类型需提供四个方向的转换，不处理流式转换：
 * 1. 端点请求 -> Unified
 * 2. Unified -> 端点请求
 * 3. 端点响应 -> Unified
 * 4. Unified -> 端点响应
 *
 * @param <V> 端点请求类型
 * @param <R> 端点响应类型
 */
public interface EndpointTransform<V, R> {
    /** 返回该转换器支持的 ProviderType 名称（枚举 name）。 */
    EndpointType type();

    // 端点请求 -> Unified
    UnifiedChatRequest endpointToUnifiedRequest(V vendorRequest);
    // Unified -> 端点请求
    V unifiedToEndpointRequest(UnifiedChatRequest unifiedRequest);
    // 端点响应 -> Unified
    UnifiedChatResponse endpointToUnifiedResponse(R vendorResponse);
    // Unified -> 端点响应
    R unifiedToEndpointResponse(UnifiedChatResponse unifiedResponse);
}

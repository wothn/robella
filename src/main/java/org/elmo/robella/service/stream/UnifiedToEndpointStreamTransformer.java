package org.elmo.robella.service.stream;

import org.elmo.robella.model.internal.UnifiedStreamChunk;

import java.util.stream.Stream;

/**
 * 将统一格式的流式响应转换为端点特定格式的流式响应
 * @param <T> 端点特定的流式响应类型
 */
public interface UnifiedToEndpointStreamTransformer<T> {

    /**
     * 将统一格式的流式响应转换为端点特定格式的流式响应
     * @param unifiedStream 统一格式的流式响应
     * @param sessionId 会话ID，用于状态管理
     * @return 端点特定格式的流式响应
     */
    Stream<T> transform(Stream<UnifiedStreamChunk> unifiedStream, String sessionId);

}
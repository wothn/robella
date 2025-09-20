package org.elmo.robella.service.stream;

import org.elmo.robella.model.internal.UnifiedStreamChunk;
import reactor.core.publisher.Flux;

/**
 * 将厂商特定的流式响应转换为统一格式的流式响应
 * @param <T> 厂商特定的流式响应类型
 */
public interface EndpointToUnifiedStreamTransformer<T> {

    /**
     * 将厂商特定的流式响应转换为统一格式的流式响应
     * @param vendorStream 厂商特定的流式响应
     * @param sessionId 会话ID，用于状态管理
     * @return 统一格式的流式响应
     */
    Flux<UnifiedStreamChunk> transform(Flux<T> vendorStream, String sessionId);

}
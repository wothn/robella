package org.elmo.robella.client;

import org.elmo.robella.model.entity.Provider;
import org.elmo.robella.model.internal.UnifiedChatRequest;
import org.elmo.robella.model.internal.UnifiedChatResponse;
import org.elmo.robella.model.internal.UnifiedStreamChunk;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * API 客户端接口
 * Client 实例应该是无状态的，通过 Provider 参数获取配置信息
 */
public interface ApiClient {

    /**
     * 发送统一的聊天请求，并返回响应的 Mono。
     *
     * @param request 统一聊天请求对象
     * @param provider 提供商配置信息
     * @return Mono，包含统一聊天响应
     */
    Mono<UnifiedChatResponse> chatCompletion(UnifiedChatRequest request, Provider provider);

    /**
     * 以流式方式发送统一的聊天请求，返回响应字符串的 Flux。
     *
     * @param request 统一聊天请求对象
     * @param provider 提供商配置信息
     * @return Flux，包含响应字符串流
     */
    Flux<UnifiedStreamChunk> streamChatCompletion(UnifiedChatRequest request, Provider provider);
}
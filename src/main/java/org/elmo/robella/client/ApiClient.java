package org.elmo.robella.client;

import org.elmo.robella.model.entity.Provider;
import org.elmo.robella.model.internal.UnifiedChatRequest;
import org.elmo.robella.model.internal.UnifiedChatResponse;
import org.elmo.robella.model.internal.UnifiedStreamChunk;

import java.util.stream.Stream;

/**
 * API 客户端接口
 * Client 实例应该是无状态的，通过 Provider 参数获取配置信息
 */
public interface ApiClient {

    /**
     * 发送统一的聊天请求，并返回响应
     *
     * @param request 统一聊天请求对象
     * @param provider 提供商配置信息
     * @return 统一聊天响应
     */
    UnifiedChatResponse chat(UnifiedChatRequest request, Provider provider);

    /**
     * 以流式方式发送统一的聊天请求，返回响应字符串列表
     *
     * @param request 统一聊天请求对象
     * @param provider 提供商配置信息
     * @return 响应字符串列表
     */
    Stream<UnifiedStreamChunk> chatStream(UnifiedChatRequest request, Provider provider);



}
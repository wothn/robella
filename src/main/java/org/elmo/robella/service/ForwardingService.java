package org.elmo.robella.service;

import org.elmo.robella.model.internal.UnifiedChatRequest;
import org.elmo.robella.model.internal.UnifiedChatResponse;
import org.elmo.robella.model.openai.model.ModelListResponse;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public interface ForwardingService {
    /**
     * 获取模型列表
     */
    Mono<ModelListResponse> listModels();
    
    /**
     * 刷新模型缓存
     */
    void refreshModelCache();

    // ===== Unified =====
    /**
     * 非流式接口
     */
    Mono<UnifiedChatResponse> forwardUnified(UnifiedChatRequest request, String providerName);

    /**
     * 带端点族信息的流式接口
     * @param request 统一格式的聊天请求
     * @param providerName 后端提供商名称（可选，为null时自动选择）
     * @param endpointFormat 端点格式（如 "OpenAI" 或 "Anthropic"），指定返回的流式响应格式
     */
    Flux<String> streamUnified(UnifiedChatRequest request, String providerName, String endpointFormat);

}
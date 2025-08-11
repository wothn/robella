package org.elmo.robella.service;

import org.elmo.robella.model.openai.ChatCompletionRequest;
import org.elmo.robella.model.openai.ChatCompletionResponse;
import org.elmo.robella.model.openai.ModelListResponse;
import org.elmo.robella.model.internal.UnifiedChatRequest;
import org.elmo.robella.model.internal.UnifiedChatResponse;
import org.elmo.robella.model.internal.UnifiedStreamChunk;
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
    Mono<UnifiedChatResponse> forwardUnified(UnifiedChatRequest request, String forcedProviderTypeOrName);
    /**
     * 流式接口
     */
    Flux<UnifiedStreamChunk> streamUnified(UnifiedChatRequest request, String forcedProviderTypeOrName);

}
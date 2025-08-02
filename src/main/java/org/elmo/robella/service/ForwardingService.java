package org.elmo.robella.service;

import org.elmo.robella.model.openai.ChatCompletionRequest;
import org.elmo.robella.model.openai.ChatCompletionResponse;
import org.elmo.robella.model.openai.ModelListResponse;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public interface ForwardingService {
    /**
     * 非流式聊天完成
     */
    Mono<ChatCompletionResponse> forwardChatCompletion(ChatCompletionRequest request);

    /**
     * 流式聊天完成
     */
    Flux<String> streamChatCompletion(ChatCompletionRequest request);

    /**
     * 获取模型列表
     */
    Mono<ModelListResponse> listModels();
    
    /**
     * 刷新模型缓存
     */
    void refreshModelCache();

}
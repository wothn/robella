package org.elmo.robella.service;

import org.elmo.robella.model.request.OpenAIChatRequest;
import org.elmo.robella.model.response.OpenAIChatResponse;
import org.elmo.robella.model.response.OpenAIModelListResponse;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public interface ForwardingService {
    /**
     * 非流式聊天完成
     */
    Mono<OpenAIChatResponse> forwardChatCompletion(OpenAIChatRequest request);

    /**
     * 流式聊天完成
     */
    Flux<ServerSentEvent<String>> streamChatCompletion(OpenAIChatRequest request);

    /**
     * 获取模型列表
     */
    Mono<OpenAIModelListResponse> listModels();
}
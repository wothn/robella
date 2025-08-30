package org.elmo.robella.client;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ApiClient {
    /**
     * 非流式聊天完成
     */
    Mono<?> chatCompletion(Object request);

    /**
     * 流式聊天完成
     */
    Flux<?> streamChatCompletion(Object request);
}
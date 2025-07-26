package org.elmo.robella.adapter;

import org.elmo.robella.model.common.ModelInfo;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface AIProviderAdapter {
    /**
     * 非流式聊天完成
     */
    Mono<?> chatCompletion(Object request);

    /**
     * 流式聊天完成
     */
    Flux<?> streamChatCompletion(Object request);

    /**
     * 获取支持的模型列表
     */
    Mono<List<ModelInfo>> listModels();

    /**
     * 获取提供商名称
     */
    String getProviderName();
}
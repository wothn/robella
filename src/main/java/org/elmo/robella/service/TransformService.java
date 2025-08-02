package org.elmo.robella.service;

import org.elmo.robella.model.openai.ChatCompletionRequest;
import org.elmo.robella.model.openai.ChatCompletionResponse;
import org.springframework.stereotype.Service;

@Service
public interface TransformService {
    /**
     * OpenAI请求转提供商特定格式
     */
    Object toVendor(ChatCompletionRequest request, String providerName);

    /**
     * 提供商响应转OpenAI响应
     */
    ChatCompletionResponse toOpenAI(Object vendorResponse, String providerName);

    /**
     * 提供商流事件转OpenAI流事件JSON数据（不包含SSE格式）
     */
    String toOpenAIStreamEvent(Object vendorStreamEvent, String providerName);
}
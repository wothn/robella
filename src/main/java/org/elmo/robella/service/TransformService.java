package org.elmo.robella.service;

import org.elmo.robella.model.request.OpenAIChatRequest;
import org.elmo.robella.model.request.UnifiedChatRequest;
import org.elmo.robella.model.response.UnifiedChatResponse;
import org.elmo.robella.model.response.OpenAIChatResponse;
import org.springframework.stereotype.Service;

@Service
public interface TransformService {
    /**
     * OpenAI请求转统一格式
     */
    UnifiedChatRequest toUnified(OpenAIChatRequest openaiRequest);

    /**
     * 统一格式转OpenAI响应
     */
    OpenAIChatResponse toOpenAI(UnifiedChatResponse unifiedResponse);

    /**
     * 统一格式转提供商特定格式
     */
    Object toVendor(UnifiedChatRequest unifiedRequest, String providerName);

    /**
     * 提供商流事件转OpenAI流事件
     */
    Object toOpenAIStreamEvent(Object vendorStreamEvent);
}
package org.elmo.robella.service.transform;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.elmo.robella.model.common.EndpointType;
import org.elmo.robella.model.internal.*;
import org.elmo.robella.model.openai.core.ChatCompletionRequest;
import org.elmo.robella.model.openai.core.ChatCompletionResponse;
import org.elmo.robella.util.OpenAITransformUtils;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * OpenAI 及 OpenAI 兼容（DeepSeek、ModelScope、AIHubMix、Azure OpenAI）转换实现。不处理流式转换。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAITransform implements VendorTransform<ChatCompletionRequest, ChatCompletionResponse> {

    @Override
    public EndpointType type() {
        return EndpointType.OPENAI;
    }

    @Override
    public UnifiedChatRequest endpointToUnifiedRequest(ChatCompletionRequest vendorRequest) {
        if (vendorRequest == null) {
            return null;
        }
        ChatCompletionRequest req = vendorRequest;

        // 使用工具类获取已设置基础字段的对象
        UnifiedChatRequest unifiedRequest = new UnifiedChatRequest();
        OpenAITransformUtils.convertBaseToUnified(req, unifiedRequest);

        // 转换消息列表
        if (req.getMessages() != null) {
            unifiedRequest.setMessages(req.getMessages());
        }

        // 转换工具列表
        if (req.getTools() != null && !req.getTools().isEmpty()) {
            unifiedRequest.setTools(req.getTools());
        }

        // 转换工具选择
        unifiedRequest.setToolChoice(req.getToolChoice());

        // 思考参数映射
        OpenAITransformUtils.convertThinkingToUnified(req, unifiedRequest);

        // 处理厂商特定参数
        Map<String, Object> vendorExtras = new HashMap<>();
        if (req.getExtraBody() != null) {
            vendorExtras.put("openai.extra_body", req.getExtraBody());
        }
        if (!vendorExtras.isEmpty()) {
            unifiedRequest.setVendorExtras(vendorExtras);
        }

        return unifiedRequest;
    }

    @Override
    public ChatCompletionRequest unifiedToEndpointRequest(UnifiedChatRequest unifiedRequest) {

        ChatCompletionRequest chatRequest = new ChatCompletionRequest();
        OpenAITransformUtils.convertUnifiedToBase(unifiedRequest, chatRequest);

        // 转换消息列表
        if (unifiedRequest.getMessages() != null) {
            chatRequest.setMessages(unifiedRequest.getMessages());
        }

        // 转换工具列表
        if (unifiedRequest.getTools() != null && !unifiedRequest.getTools().isEmpty()){
            chatRequest.setTools(unifiedRequest.getTools());
        }

        // 转换工具选择
        chatRequest.setToolChoice(unifiedRequest.getToolChoice());

        // 思考参数映射
        if (unifiedRequest.getThinkingOptions() != null) {
            OpenAITransformUtils.convertThinkingToChat(unifiedRequest,  chatRequest);
        }

        // 处理厂商特定参数
        if (unifiedRequest.getVendorExtras() != null) {
            Object extraBody = unifiedRequest.getVendorExtras().get("openai.extra_body");
            if (extraBody != null) {
                chatRequest.setExtraBody(extraBody);
            }
        }

        return chatRequest;
    }

    @Override
    public UnifiedChatResponse endpointToUnifiedResponse(ChatCompletionResponse vendorResponse) {
        if (vendorResponse == null) {
            return null;
        }
        ChatCompletionResponse resp = vendorResponse;

        UnifiedChatResponse unifiedResponse = new UnifiedChatResponse();
        unifiedResponse.setId(resp.getId());
        unifiedResponse.setModel(resp.getModel());
        unifiedResponse.setCreated(resp.getCreated());
        unifiedResponse.setSystemFingerprint(resp.getSystemFingerprint());

        // 转换Usage
        unifiedResponse.setUsage(resp.getUsage());

        // 转换Choice
        unifiedResponse.setChoices(resp.getChoices());

        return unifiedResponse;
    }

    @Override
    public ChatCompletionResponse unifiedToEndpointResponse(UnifiedChatResponse unifiedResponse) {
        if (unifiedResponse == null) {
            return null;
        }

        ChatCompletionResponse chatResponse = new ChatCompletionResponse();
        chatResponse.setId(unifiedResponse.getId());
        chatResponse.setObject(unifiedResponse.getObject());
        chatResponse.setCreated(unifiedResponse.getCreated());
        chatResponse.setModel(unifiedResponse.getModel());
        chatResponse.setSystemFingerprint(unifiedResponse.getSystemFingerprint());

        // 转换Usage
        chatResponse.setUsage(unifiedResponse.getUsage());

        // 转换Choice
        chatResponse.setChoices(unifiedResponse.getChoices());
        return chatResponse;
    }
}
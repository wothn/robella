package org.elmo.robella.service.transform;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.elmo.robella.config.ProviderType;
import org.elmo.robella.model.internal.*;
import org.elmo.robella.model.openai.core.ChatCompletionRequest;
import org.elmo.robella.model.openai.core.ChatCompletionResponse;
import org.elmo.robella.model.openai.stream.ChatCompletionChunk;
import org.elmo.robella.service.VendorTransform;
import org.elmo.robella.util.ConfigUtils;
import org.elmo.robella.util.JsonUtils;
import org.elmo.robella.util.OpenAITransformUtils;

import java.util.*;

/**
 * OpenAI 及 OpenAI 兼容（DeepSeek、ModelScope、AIHubMix、Azure OpenAI）转换实现。
 */
@Slf4j
@RequiredArgsConstructor
public class OpenAITransform implements VendorTransform {
    
    private final ConfigUtils configUtils;
    @Override
    public String type() {
        return ProviderType.OpenAI.getName();
    }

    @Override
    public UnifiedChatRequest vendorRequestToUnified(Object vendorRequest) {
        if (!(vendorRequest instanceof ChatCompletionRequest req)) {
            return null;
        }

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

        // 获取配置的thinkingField
        String thinkingField = configUtils.getThinkingField(unifiedRequest.getProviderName(), unifiedRequest.getModel());
        unifiedRequest.getTempFields().put("config_thinking", thinkingField);

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

        // 处理未知字段
        Map<String, Object> undefined = req.getUndefined();
        if (undefined != null && !undefined.isEmpty()) {
            unifiedRequest.setUndefined(undefined);
        }

        return unifiedRequest;
    }

    @Override
    public Object unifiedToVendorRequest(UnifiedChatRequest unifiedRequest) {

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

        // 处理未知字段
        if (unifiedRequest.getUndefined() != null && !unifiedRequest.getUndefined().isEmpty()) {
            chatRequest.setUndefined(unifiedRequest.getUndefined());
        }

        return chatRequest;
    }

    @Override
    public UnifiedChatResponse vendorResponseToUnified(Object vendorResponse) {
        if (!(vendorResponse instanceof ChatCompletionResponse resp)) {
            return null;
        }

        UnifiedChatResponse unifiedResponse = new UnifiedChatResponse();
        unifiedResponse.setId(resp.getId());
        unifiedResponse.setModel(resp.getModel());
        unifiedResponse.setCreated(resp.getCreated());
        unifiedResponse.setSystemFingerprint(resp.getSystemFingerprint());

        // 转换Usage
        unifiedResponse.setUsage(resp.getUsage());

        // 转换Choice
        unifiedResponse.setChoices(resp.getChoices());

        // 为undefined赋值
        if (resp.getUndefined() != null) {
            unifiedResponse.setUndefined(resp.getUndefined());
        }
        return unifiedResponse;
    }

    @Override
    public Object unifiedToVendorResponse(UnifiedChatResponse unifiedResponse) {
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

        // 为undefined赋值
        if (unifiedResponse.getUndefined() != null) {
            chatResponse.setUndefined(unifiedResponse.getUndefined());
        }
        return chatResponse;
    }

    @Override
    public UnifiedStreamChunk vendorStreamEventToUnified(Object vendorEvent) {
        if (!(vendorEvent instanceof ChatCompletionChunk chunk)) {
            log.warn("vendorStreamEventToUnified: vendorEvent is not instance of ChatCompletionChunk, type: {}",
                    vendorEvent != null ? vendorEvent.getClass().getName() : "null");
            return null;
        }

        UnifiedStreamChunk unifiedChunk = new UnifiedStreamChunk();
        unifiedChunk.setId(chunk.getId());
        unifiedChunk.setCreated(chunk.getCreated());
        unifiedChunk.setModel(chunk.getModel());
        unifiedChunk.setObject(chunk.getObject());
        unifiedChunk.setSystemFingerprint(chunk.getSystemFingerprint());
        unifiedChunk.setChoices(chunk.getChoices());
        unifiedChunk.setUsage(chunk.getUsage());
        
        return unifiedChunk;
    }

    @Override
    public String unifiedStreamChunkToVendor(UnifiedStreamChunk chunk) {
        if (chunk == null) {
            return null;
        }

        ChatCompletionChunk chatCompletionChunk = new ChatCompletionChunk();
        chatCompletionChunk.setId(chunk.getId());
        chatCompletionChunk.setCreated(chunk.getCreated());
        chatCompletionChunk.setModel(chunk.getModel());
        chatCompletionChunk.setObject(chunk.getObject());
        chatCompletionChunk.setSystemFingerprint(chunk.getSystemFingerprint());

        // 转换choices
        if (chunk.getChoices() != null) {
            chatCompletionChunk.setChoices(chunk.getChoices());
        }

        // 转换usage
        if (chunk.getUsage() != null) {
            chatCompletionChunk.setUsage(chunk.getUsage());
        }

        return JsonUtils.toJson(chatCompletionChunk);
    }
}
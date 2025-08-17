package org.elmo.robella.service.transform;

import lombok.extern.slf4j.Slf4j;

import org.elmo.robella.config.ProviderType;
import org.elmo.robella.model.internal.*;
import org.elmo.robella.model.openai.*;
import org.elmo.robella.service.VendorTransform;
import org.elmo.robella.util.OpenAITransformUtils;

import java.util.*;

/**
 * OpenAI 及 OpenAI 兼容（DeepSeek、ModelScope、AIHubMix、Azure OpenAI）转换实现。
 */
@Slf4j
public class OpenAITransform implements VendorTransform {
    @Override
    public String type() {
        return ProviderType.OpenAI.getName();
    }

    @Override
    public UnifiedChatRequest vendorRequestToUnified(Object vendorRequest) {
        if (!(vendorRequest instanceof ChatCompletionRequest req)) {
            return null;
        }

        // 能直接对应赋值的先赋值
        UnifiedChatRequest.UnifiedChatRequestBuilder builder = OpenAITransformUtils.convertBaseToUnified(req);

        // 转换消息列表
        if (req.getMessages() != null) {
            builder.messages(req.getMessages());
        }

        // 转换工具列表
        if (req.getTools() != null && !req.getTools().isEmpty()) {
            builder.tools(req.getTools());
        }

        // 转换工具选择
        builder.toolChoice(req.getToolChoice());

        // 创建 tempFields 用于保存转换过程中的状态
        Map<String, Object> tempFields = new HashMap<>();
        
        // 思考参数映射
        UnifiedChatRequest.ThinkingOptions thinkingOptions = OpenAITransformUtils.convertThinkingToUnified(req, tempFields);
        if (thinkingOptions != null) {
            builder.thinkingOptions(thinkingOptions);
        }

        // 处理厂商特定参数
        Map<String, Object> vendorExtras = new HashMap<>();
        if (req.getExtraBody() != null) {
            vendorExtras.put("openai.extra_body", req.getExtraBody());
        }
        if (!vendorExtras.isEmpty()) {
            builder.vendorExtras(vendorExtras);
        }

        // 处理未知字段
        Map<String, Object> undefined = req.getUndefined();
        if (undefined != null && !undefined.isEmpty()) {
            builder.undefined(undefined);
        }

        // 设置 tempFields（在最后统一设置，避免覆盖）
        if (!tempFields.isEmpty()) {
            builder.tempFields(tempFields);
        }

        return builder.build();
    }

    @Override
    public Object unifiedToVendorRequest(UnifiedChatRequest unifiedRequest) {

        ChatCompletionRequest.ChatCompletionRequestBuilder builder = OpenAITransformUtils.convertUnifiedToBase(unifiedRequest);

        // 转换消息列表
        if (unifiedRequest.getMessages() != null) {
            builder.messages(unifiedRequest.getMessages());
        }

        // 转换工具列表
        if (unifiedRequest.getTools() != null && !unifiedRequest.getTools().isEmpty()){
            builder.tools(unifiedRequest.getTools());
        }

        // 转换工具选择
        builder.toolChoice(unifiedRequest.getToolChoice());

        // 思考参数映射
        if (unifiedRequest.getThinkingOptions() != null) {
            OpenAITransformUtils.convertThinkingToChat(unifiedRequest,  builder);
        }

        // 处理厂商特定参数
        if (unifiedRequest.getVendorExtras() != null) {
            Object extraBody = unifiedRequest.getVendorExtras().get("openai.extra_body");
            if (extraBody != null) {
                builder.extraBody(extraBody);
            }
        }

        // 处理未知字段
        if (unifiedRequest.getUndefined() != null && !unifiedRequest.getUndefined().isEmpty()) {
            builder.undefined(unifiedRequest.getUndefined());
        }

        return builder.build();
    }

    @Override
    public UnifiedChatResponse vendorResponseToUnified(Object vendorResponse) {
        if (!(vendorResponse instanceof ChatCompletionResponse resp)) {
            return null;
        }

        UnifiedChatResponse.UnifiedChatResponseBuilder builder = UnifiedChatResponse.builder()
                .id(resp.getId())
                .model(resp.getModel())
                .created(resp.getCreated())
                .systemFingerprint(resp.getSystemFingerprint());

        // 转换Usage
        builder.usage(resp.getUsage());

        // 转换Choice
        builder.choices(resp.getChoices());

        // 为undefined赋值
        if (resp.getUndefined() != null) {
            builder.undefined(resp.getUndefined());
        }
        return builder.build();
    }

    @Override
    public Object unifiedToVendorResponse(UnifiedChatResponse unifiedResponse) {
        if (unifiedResponse == null) {
            return null;
        }

        ChatCompletionResponse.ChatCompletionResponseBuilder builder = ChatCompletionResponse.builder()
                .id(unifiedResponse.getId())
                .object(unifiedResponse.getObject())
                .created(unifiedResponse.getCreated())
                .model(unifiedResponse.getModel())
                .systemFingerprint(unifiedResponse.getSystemFingerprint());

        // 转换Usage
        builder.usage(unifiedResponse.getUsage());

        // 转换Choice
        builder.choices(unifiedResponse.getChoices());

        // 为undefined赋值
        if (unifiedResponse.getUndefined() != null) {
            builder.undefined(unifiedResponse.getUndefined());
        }
        return builder.build();
    }

    @Override
    public UnifiedStreamChunk vendorStreamEventToUnified(Object vendorEvent) {
        if (vendorEvent == null) {
            return null;
        }

        if (!(vendorEvent instanceof ChatCompletionChunk chunk)) {
            return null;
        }

        UnifiedStreamChunk.UnifiedStreamChunkBuilder builder = UnifiedStreamChunk.builder()
                .id(chunk.getId())
                .created(chunk.getCreated())
                .model(chunk.getModel())
                .object(chunk.getObject())
                .systemFingerprint(chunk.getSystemFingerprint());

        // 转换choices
        if (chunk.getChoices() != null) {
            builder.choices(chunk.getChoices());
        }

        // 转换usage
        if (chunk.getUsage() != null) {
            builder.usage(chunk.getUsage());
        }

        return builder.build();
    }

    @Override
    public Object unifiedStreamChunkToVendor(UnifiedStreamChunk chunk) {
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

        return chatCompletionChunk;
    }
}
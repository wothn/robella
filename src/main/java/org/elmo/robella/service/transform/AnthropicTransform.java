package org.elmo.robella.service.transform;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.elmo.robella.config.ProviderType;
import org.elmo.robella.model.internal.*;
import org.elmo.robella.service.VendorTransform;
import org.elmo.robella.util.ConfigUtils;

import org.elmo.robella.model.anthropic.core.*;
import org.elmo.robella.util.AnthropicTransformUtils;


/**
 * Anthropic Messages API 转换实现
 */
@Slf4j
@RequiredArgsConstructor
public class AnthropicTransform implements VendorTransform {
    
    private final ConfigUtils configUtils;


    @Override
    public String type() {
        return ProviderType.Anthropic.getName();
    }

    @Override
    public UnifiedChatRequest vendorRequestToUnified(Object vendorRequest) {
        if (!(vendorRequest instanceof AnthropicChatRequest req)) {
            return null;
        }
        // 设置基础字段
        UnifiedChatRequest unifiedRequest = new UnifiedChatRequest();
        AnthropicTransformUtils.convertBaseToUnified(req, unifiedRequest);

        // 设置tools
        AnthropicTransformUtils.convertToolsToUnified(req, unifiedRequest);

        // 设置tool_choice
        AnthropicTransformUtils.convertToolChoiceToUnified(req, unifiedRequest);

        // 设置思考字段
        AnthropicTransformUtils.convertThinkingToUnified(req, unifiedRequest);

        // 设置配置思考字段
        unifiedRequest.getTempFields().put("config_thinking", configUtils.getThinkingField(unifiedRequest.getProviderName(), unifiedRequest.getModel()));

        // 转换messages
        AnthropicTransformUtils.convertMessagesToUnified(req, unifiedRequest);

        // 处理系统消息（Anthropic的system字段转换为OpenAI格式的系统消息）
        AnthropicTransformUtils.convertSystemToUnified(req, unifiedRequest);

        return unifiedRequest;
    }

    @Override
    public Object unifiedToVendorRequest(UnifiedChatRequest unifiedRequest) {
        AnthropicChatRequest anthropicRequest = new AnthropicChatRequest();
        
        // 设置基础字段
        AnthropicTransformUtils.convertBaseToAnthropic(unifiedRequest, anthropicRequest);

        // 设置tools
        AnthropicTransformUtils.convertToolsToAnthropic(unifiedRequest, anthropicRequest);

        // 设置tool_choice
        AnthropicTransformUtils.convertToolChoiceToAnthropic(unifiedRequest, anthropicRequest);

        // 设置思考字段
        AnthropicTransformUtils.convertThinkingToAnthropic(unifiedRequest, anthropicRequest);

        // 转换messages
        AnthropicTransformUtils.convertMessagesToAnthropic(unifiedRequest, anthropicRequest);

        // 处理系统消息（从OpenAI格式的系统消息转换为Anthropic的system字段）
        AnthropicTransformUtils.convertSystemToAnthropic(unifiedRequest, anthropicRequest);

        return anthropicRequest;
    }


    @Override
    public UnifiedChatResponse vendorResponseToUnified(Object vendorResponse) {
        return null; // TODO: 实现 vendorResponseToUnified
    }

    @Override
    public Object unifiedToVendorResponse(UnifiedChatResponse unifiedResponse) {
        return null; // TODO: 实现 unifiedToVendorResponse
    }

    @Override
    public UnifiedStreamChunk vendorStreamEventToUnified(Object vendorEvent) {

        return null;
    }

    @Override
    public String unifiedStreamChunkToVendor(UnifiedStreamChunk chunk) {
        // TODO: 实现 Anthropic 流事件转换
        return null;
    }
    


}

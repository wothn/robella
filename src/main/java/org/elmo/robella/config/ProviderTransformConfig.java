package org.elmo.robella.config;

import org.elmo.robella.common.ProviderType;
import org.elmo.robella.model.anthropic.core.AnthropicChatRequest;
import org.elmo.robella.model.anthropic.core.AnthropicMessage;
import org.elmo.robella.model.openai.core.ChatCompletionRequest;
import org.elmo.robella.model.openai.core.ChatCompletionResponse;
import org.elmo.robella.service.transform.provider.VendorTransform;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ProviderTransform配置类
 */
@Configuration
public class ProviderTransformConfig {

    @Bean
    public Map<ProviderType, VendorTransform<ChatCompletionRequest, ChatCompletionResponse>> openaiProviderTransformMap(
            List<VendorTransform<ChatCompletionRequest, ChatCompletionResponse>> transforms) {

        Map<ProviderType, VendorTransform<ChatCompletionRequest, ChatCompletionResponse>> transformMap = new HashMap<>();

        for (VendorTransform<ChatCompletionRequest, ChatCompletionResponse> transform : transforms) {
            transformMap.put(transform.providerType(), transform);
        }

        return transformMap;
    }

    @Bean
    public Map<ProviderType, VendorTransform<AnthropicChatRequest, AnthropicMessage>> anthropicProviderTransformMap(
            List<VendorTransform<AnthropicChatRequest, AnthropicMessage>> transforms) {

        Map<ProviderType, VendorTransform<AnthropicChatRequest, AnthropicMessage>> transformMap = new HashMap<>();

        for (VendorTransform<AnthropicChatRequest, AnthropicMessage> transform : transforms) {
            transformMap.put(transform.providerType(), transform);
        }

        return transformMap;
    }
}
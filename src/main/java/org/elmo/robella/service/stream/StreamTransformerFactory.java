package org.elmo.robella.service.stream;

import org.elmo.robella.config.ProviderType;
import org.elmo.robella.service.stream.openai.OpenAIStreamToUnifiedTransformer;
import org.elmo.robella.service.stream.openai.OpenAIUnifiedToEndpointTransformer;
import org.elmo.robella.service.stream.anthropic.AnthropicStreamToUnifiedTransformer;
import org.elmo.robella.service.stream.anthropic.AnthropicUnifiedToEndpointTransformer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 流式转换器工厂类
 * 根据提供商类型获取对应的转换器实例
 */
@Component
public class StreamTransformerFactory {
    
    @Autowired
    private OpenAIStreamToUnifiedTransformer openAIStreamToUnifiedTransformer;
    
    @Autowired
    private OpenAIUnifiedToEndpointTransformer openAIUnifiedToEndpointTransformer;
    
    @Autowired
    private AnthropicStreamToUnifiedTransformer anthropicStreamToUnifiedTransformer;
    
    @Autowired
    private AnthropicUnifiedToEndpointTransformer anthropicUnifiedToEndpointTransformer;
    
    /**
     * 获取将厂商流式响应转换为统一格式的转换器
     * @param providerType 提供商类型
     * @return 转换器实例
     */
    public StreamToUnifiedTransformer<?> getStreamToUnifiedTransformer(ProviderType providerType) {
        switch (providerType) {
            case OpenAI:
                return openAIStreamToUnifiedTransformer;
            case Anthropic:
                return anthropicStreamToUnifiedTransformer;
            default:
                throw new IllegalArgumentException("Unsupported provider type: " + providerType);
        }
    }
    
    /**
     * 获取将统一格式转换为端点格式的转换器
     * @param providerType 提供商类型
     * @return 转换器实例
     */
    public UnifiedToEndpointTransformer<?> getUnifiedToEndpointTransformer(ProviderType providerType) {
        switch (providerType) {
            case OpenAI:
                return openAIUnifiedToEndpointTransformer;
            case Anthropic:
                return anthropicUnifiedToEndpointTransformer;
            default:
                throw new IllegalArgumentException("Unsupported provider type: " + providerType);
        }
    }
}
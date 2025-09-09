package org.elmo.robella.service.stream;

import org.elmo.robella.model.common.EndpointType;
import org.elmo.robella.model.internal.UnifiedStreamChunk;
import org.elmo.robella.service.stream.anthropic.AnthropicStreamToUnifiedTransformer;
import org.elmo.robella.service.stream.anthropic.AnthropicUnifiedToEndpointTransformer;
import org.elmo.robella.service.stream.openai.OpenAIStreamToUnifiedTransformer;
import org.elmo.robella.service.stream.openai.OpenAIUnifiedToEndpointTransformer;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

/**
 * 流式转换器工厂类
 * 根据提供商类型获取对应的转换器实例
 */
@Component
@RequiredArgsConstructor
public class StreamTransformerFactory {
    
    private final OpenAIStreamToUnifiedTransformer openAIStreamToUnifiedTransformer;
    
    private final OpenAIUnifiedToEndpointTransformer openAIUnifiedToEndpointTransformer;
    
    private final AnthropicStreamToUnifiedTransformer anthropicStreamToUnifiedTransformer;
    
    private final AnthropicUnifiedToEndpointTransformer anthropicUnifiedToEndpointTransformer;
    
    /**
     * 直接转换：将厂商流式响应转换为统一格式
     * @param providerType 提供商类型
     * @param vendorStream 厂商流式响应流
     * @param sessionId 会话ID
     * @return 统一格式的流式响应
     */
    public Flux<UnifiedStreamChunk> transformToUnified(EndpointType providerType, Flux<?> vendorStream, String sessionId) {
        StreamToUnifiedTransformer<?> transformer = getStreamToUnifiedTransformer(providerType);
        @SuppressWarnings("unchecked")
        Flux<UnifiedStreamChunk> result = ((StreamToUnifiedTransformer<Object>) transformer).transformToUnified((Flux<Object>) vendorStream, sessionId);
        return result;
    }
    
    /**
     * 直接转换：将统一格式转换为端点格式
     * @param providerType 提供商类型
     * @param unifiedStream 统一格式的响应流
     * @param sessionId 会话ID
     * @return 端点格式的流式响应
     */
    public Flux<String> transformToEndpoint(EndpointType providerType, Flux<UnifiedStreamChunk> unifiedStream, String sessionId) {
        UnifiedToEndpointTransformer<?> transformer = getUnifiedToEndpointTransformer(providerType);
        @SuppressWarnings("unchecked")
        Flux<String> result = (Flux<String>) transformer.transformToEndpoint(unifiedStream, sessionId);
        return result;
    }

    /**
     * 获取将厂商流式响应转换为统一格式的转换器
     * @param providerType 提供商类型
     * @return 转换器实例
     */
    public StreamToUnifiedTransformer<?> getStreamToUnifiedTransformer(EndpointType providerType) {
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
    public UnifiedToEndpointTransformer<?> getUnifiedToEndpointTransformer(EndpointType providerType) {
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
package org.elmo.robella.service.transform;

import org.elmo.robella.model.anthropic.core.AnthropicChatRequest;
import org.elmo.robella.model.anthropic.core.AnthropicMessage;
import org.elmo.robella.model.common.EndpointType;
import org.elmo.robella.model.internal.UnifiedChatRequest;
import org.elmo.robella.model.internal.UnifiedChatResponse;
import org.elmo.robella.model.openai.core.ChatCompletionRequest;
import org.elmo.robella.model.openai.core.ChatCompletionResponse;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 供应商转换工厂，根据供应商类型获取对应的转换实现。
 */
@Component
public class VendorTransformFactory {

    private final Map<EndpointType, VendorTransform<?, ?>> transformMap = new ConcurrentHashMap<>();

    

    /**
     * 直接转换：将 Unified 请求转换为厂商请求
     */
    @SuppressWarnings("unchecked")
    public <V> V unifiedToVendorRequest(EndpointType type, UnifiedChatRequest unifiedRequest) {
        return (V) get(type).unifiedToVendorRequest(unifiedRequest);
    }

    /**
     * 直接转换：将厂商请求转换为 Unified 请求
     */
    @SuppressWarnings("unchecked")
    public <V> UnifiedChatRequest vendorRequestToUnified(EndpointType type, V vendorRequest) {
        return ((VendorTransform<V, ?>) get(type)).vendorRequestToUnified(vendorRequest);
    }

    /**
     * 直接转换：将 Unified 响应转换为厂商响应
     */
    @SuppressWarnings("unchecked")
    public <R> R unifiedToVendorResponse(EndpointType type, UnifiedChatResponse unifiedResponse) {
        return (R) get(type).unifiedToVendorResponse(unifiedResponse);
    }

    /**
     * 直接转换：将厂商响应转换为 Unified 响应
     */
    @SuppressWarnings("unchecked")
    public <R> UnifiedChatResponse vendorResponseToUnified(EndpointType type, R vendorResponse) {
        return ((VendorTransform<?, R>) get(type)).vendorResponseToUnified(vendorResponse);
    }

    public VendorTransform<?, ?> get(EndpointType type) {
        VendorTransform<?, ?> transform = transformMap.get(type);
        if (transform != null) {
            return transform;
        }
        transform = switch (type) {
            case OpenAI-> new OpenAITransform();
            case Anthropic -> new AnthropicTransform();
        };
        transformMap.put(type, transform);
        return transform;
    }

    /**
     * 获取特定类型的转换器
     */
    @SuppressWarnings("unchecked")
    public <V, R> VendorTransform<V, R> getTransform(EndpointType type) {
        return (VendorTransform<V, R>) get(type);
    }

    /**
     * 获取 OpenAI 转换器
     */
    public VendorTransform<ChatCompletionRequest, ChatCompletionResponse> getOpenAITransform() {
        return getTransform(EndpointType.OpenAI);
    }

    /**
     * 获取 Anthropic 转换器
     */
    public VendorTransform<AnthropicChatRequest, AnthropicMessage> getAnthropicTransform() {
        return getTransform(EndpointType.Anthropic);
    }
}

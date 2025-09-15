package org.elmo.robella.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elmo.robella.client.anthropic.AnthropicClient;
import org.elmo.robella.client.openai.OpenAIClient;
import org.elmo.robella.common.EndpointType;
import org.springframework.stereotype.Component;

/**
 * Client 工厂类
 * 负责根据 EndpointType 获取对应的 ApiClient 实例
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClientFactory {

    private final OpenAIClient openAIClient;
    private final AnthropicClient anthropicClient;

    /**
     * 根据端点类型获取 ApiClient 实例
     * 
     * @param endpointType 端点类型
     * @return ApiClient 实例
     * @throws IllegalArgumentException 如果不支持该端点类型
     */
    public ApiClient getClient(EndpointType endpointType) {
        log.debug("Getting client for endpoint type: {}", endpointType);
        
        return switch (endpointType) {
            case OPENAI -> openAIClient;
            case ANTHROPIC -> anthropicClient;
            default -> throw new IllegalArgumentException("Unsupported endpoint type: " + endpointType);
        };
    }
}
package org.elmo.robella.client.anthropic;

import lombok.RequiredArgsConstructor;
import org.elmo.robella.client.ApiClient;
import org.elmo.robella.client.ClientBuilder;
import org.elmo.robella.config.WebClientProperties;
import org.elmo.robella.model.anthropic.core.AnthropicChatRequest;
import org.elmo.robella.model.anthropic.core.AnthropicMessage;
import org.elmo.robella.service.stream.EndpointToUnifiedStreamTransformer;
import org.elmo.robella.service.transform.VendorTransform;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Anthropic Client 构建器
 */
@Component
@RequiredArgsConstructor
@Qualifier("ANTHROPIC")
public class AnthropicClientBuilder implements ClientBuilder {

    private final WebClient webClient;
    private final WebClientProperties webClientProperties;
    private final VendorTransform<AnthropicChatRequest, AnthropicMessage> anthropicTransform;
    private final EndpointToUnifiedStreamTransformer<Object> streamTransformer;

    @Override
    public ApiClient build() {
        return new AnthropicClient(webClient, webClientProperties, anthropicTransform, streamTransformer);
    }
}
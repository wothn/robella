package org.elmo.robella.client.openai;

import lombok.RequiredArgsConstructor;
import org.elmo.robella.client.ApiClient;
import org.elmo.robella.client.ClientBuilder;
import org.elmo.robella.config.WebClientProperties;
import org.elmo.robella.model.openai.core.ChatCompletionRequest;
import org.elmo.robella.model.openai.core.ChatCompletionResponse;
import org.elmo.robella.model.openai.stream.ChatCompletionChunk;
import org.elmo.robella.service.stream.EndpointToUnifiedStreamTransformer;
import org.elmo.robella.service.transform.VendorTransform;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * OpenAI Client 构建器
 */
@Component
@RequiredArgsConstructor
@Qualifier("OPENAI")
public class OpenAIClientBuilder implements ClientBuilder {

    private final WebClient webClient;
    private final WebClientProperties webClientProperties;
    private final VendorTransform<ChatCompletionRequest, ChatCompletionResponse> openAITransform;
    private final EndpointToUnifiedStreamTransformer<ChatCompletionChunk> streamTransformer;

    @Override
    public ApiClient build() {
        return new OpenAIClient(webClient, webClientProperties, openAITransform, streamTransformer);
    }
}
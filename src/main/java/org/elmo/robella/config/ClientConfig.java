package org.elmo.robella.config;

import org.elmo.robella.client.ClientBuilder;
import org.elmo.robella.client.anthropic.AnthropicClientBuilder;
import org.elmo.robella.client.openai.OpenAIClientBuilder;
import org.elmo.robella.model.common.EndpointType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * Client 配置类
 * 负责将 ClientBuilder 映射到对应的 EndpointType
 */
@Configuration
public class ClientConfig {

    @Bean
    public Map<EndpointType, ClientBuilder> clientBuilderMap(
            OpenAIClientBuilder openAIClientBuilder,
            AnthropicClientBuilder anthropicClientBuilder) {
        
        return Map.of(
            EndpointType.OPENAI, openAIClientBuilder,
            EndpointType.ANTHROPIC, anthropicClientBuilder
        );
    }
}
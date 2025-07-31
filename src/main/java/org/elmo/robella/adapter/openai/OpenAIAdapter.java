package org.elmo.robella.adapter.openai_compatible;

import org.elmo.robella.adapter.AIProviderAdapter;
import org.elmo.robella.config.ProviderConfig;
import org.elmo.robella.model.common.ModelInfo;
import org.elmo.robella.model.request.OpenAIChatRequest;
import org.elmo.robella.model.response.OpenAIChatResponse;
import org.elmo.robella.model.response.OpenAIModelListResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public class OpenAICompatibleAdapter implements AIProviderAdapter {
    private final ProviderConfig.Provider config;
    private final WebClient webClient;

    public OpenAICompatibleAdapter(ProviderConfig.Provider config, WebClient webClient) {
        this.config = config;
        this.webClient = webClient;
    }

    @Override
    public Mono<?> chatCompletion(Object request) {
        // 转换为OpenAI请求
        OpenAIChatRequest openaiRequest = (OpenAIChatRequest) request;
        
        // 构建URL，Azure OpenAI的URL格式不同
        String url = config.getBaseUrl();
        if ("AzureOpenAI".equals(config.getType()) && config.getDeploymentName() != null) {
            // Azure OpenAI使用/deployments/{deployment-name}/chat/completions格式
            url = url + "/deployments/" + config.getDeploymentName() + "/chat/completions?api-version=2024-02-15-preview";
        } else {
            // 标准OpenAI格式
            url = url + "/chat/completions";
        }
        
        // 构建WebClient
        WebClient client = webClient.mutate()
                .defaultHeader("Authorization", "Bearer " + config.getApiKey())
                .defaultHeader("Content-Type", "application/json")
                .build();
        
        // 发送请求到服务商API
        return client.post()
                .uri(url)
                .bodyValue(openaiRequest)
                .retrieve()
                .bodyToMono(OpenAIChatResponse.class)
                .onErrorMap(e -> new RuntimeException("Error calling OpenAI compatible API: " + e.getMessage(), e));
    }

    @Override
    public Flux<?> streamChatCompletion(Object request) {
        // 转换为OpenAI请求
        OpenAIChatRequest openaiRequest = (OpenAIChatRequest) request;
        openaiRequest.setStream(true);
        
        // 构建URL，Azure OpenAI的URL格式不同
        String url = config.getBaseUrl();
        if ("AzureOpenAI".equals(config.getType()) && config.getDeploymentName() != null) {
            // Azure OpenAI使用/deployments/{deployment-name}/chat/completions格式
            url = url + "/deployments/" + config.getDeploymentName() + "/chat/completions?api-version=2024-02-15-preview";
        } else {
            // 标准OpenAI格式
            url = url + "/chat/completions";
        }
        
        // 构建WebClient
        WebClient client = webClient.mutate()
                .defaultHeader("Authorization", "Bearer " + config.getApiKey())
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("Accept", "text/event-stream")
                .build();
        
        // 发送流式请求到服务商API
        return client.post()
                .uri(url)
                .bodyValue(openaiRequest)
                .retrieve()
                .bodyToFlux(String.class)
                .map(data -> "[DATA] " + data) // 简化的处理，实际应该更复杂
                .onErrorMap(e -> new RuntimeException("Error calling OpenAI compatible API: " + e.getMessage(), e));
    }

    @Override
    public Mono<List<ModelInfo>> listModels() {
        // 构建URL，Azure OpenAI没有标准的/models端点
        String url = config.getBaseUrl();
        if ("AzureOpenAI".equals(config.getType())) {
            // Azure OpenAI不支持列出模型
            return Mono.just(List.of());
        } else {
            // 标准OpenAI格式
            url = url + "/models";
        }
        
        // 构建WebClient
        WebClient client = webClient.mutate()
                .defaultHeader("Authorization", "Bearer " + config.getApiKey())
                .build();
        
        // OpenAI兼容API通常有/models端点
        return client.get()
                .uri(url)
                .retrieve()
                .bodyToMono(OpenAIModelListResponse.class)
                .map(response -> response.getData().stream()
                        .map(model -> {
                            ModelInfo info = new ModelInfo();
                            info.setId(model.getId());
                            info.setName(model.getId());
                            return info;
                        })
                        .toList())
                .onErrorReturn(List.of());
    }

    @Override
    public String getProviderName() {
        return config.getName();
    }
}
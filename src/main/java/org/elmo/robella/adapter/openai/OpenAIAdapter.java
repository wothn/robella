package org.elmo.robella.adapter.openai;

import lombok.extern.slf4j.Slf4j;
import org.elmo.robella.adapter.AIProviderAdapter;
import org.elmo.robella.config.ProviderConfig;
import org.elmo.robella.config.WebClientProperties;
import org.elmo.robella.exception.ProviderException;
import org.elmo.robella.model.openai.ChatCompletionRequest;
import org.elmo.robella.model.openai.ChatCompletionResponse;
import org.elmo.robella.model.openai.ModelInfo;
import org.elmo.robella.util.JsonUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

@Slf4j
public class OpenAIAdapter implements AIProviderAdapter {
    private final ProviderConfig.Provider config;
    private final WebClient webClient;
    private final WebClientProperties webClientProperties;

    public OpenAIAdapter(ProviderConfig.Provider config, WebClient webClient, WebClientProperties webClientProperties) {
        this.config = config;
        this.webClient = configureWebClient(webClient);
        this.webClientProperties = webClientProperties;
    }

    /**
     * 配置特定于OpenAI的WebClient
     */
    private WebClient configureWebClient(WebClient baseWebClient) {
        return baseWebClient.mutate()
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + config.getApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.USER_AGENT, "Robella")
                .build();
    }

    @Override
    public Mono<ChatCompletionResponse> chatCompletion(Object request) {
        ChatCompletionRequest openaiRequest = (ChatCompletionRequest) request;

        // 构建URL
        String url = buildChatCompletionUrl();
        
        // 发送请求
        return webClient.post()
                .uri(url)
                .bodyValue(openaiRequest)
                .retrieve()
                .bodyToMono(String.class)  // 先获取为字符串
                .map(responseStr -> {
                    try {
                        // 手动反序列化JSON字符串为OpenAIChatResponse对象
                        return JsonUtils.fromJson(responseStr, ChatCompletionResponse.class);
                    } catch (Exception e) {
                        throw new ProviderException("Failed to deserialize response: " + e.getMessage(), e);
                    }
                })
                .timeout(webClientProperties.getTimeout().getRead())  // 使用配置的读超时
                .onErrorMap(WebClientResponseException.class, this::handleWebClientError)
                .onErrorMap(Exception.class, e -> new ProviderException(
                    "OpenAI API call failed: " + e.getMessage(), e));
    }

    @Override
    public Flux<?> streamChatCompletion(Object request) {
        log.info("开始准备发起请求");
        ChatCompletionRequest openaiRequest = (ChatCompletionRequest) request;
        openaiRequest.setStream(true);
        
        String url = buildChatCompletionUrl();
        
        // 流式请求通常需要更长的超时时间，使用读超时的5倍
        Duration streamTimeout = webClientProperties.getTimeout().getRead().multipliedBy(5);
        
        return webClient.post()
                .uri(url)
                .header(HttpHeaders.ACCEPT, "text/event-stream")
                .bodyValue(openaiRequest)
                .retrieve()
                .bodyToFlux(String.class)
                .timeout(streamTimeout)  // 使用配置化的流式超时
                .onErrorMap(WebClientResponseException.class, this::handleWebClientError)
                .onErrorMap(Exception.class, e -> new ProviderException(
                    "OpenAI streaming API call failed: " + e.getMessage(), e));
    }

    @Override
    public Mono<List<ModelInfo>> listModels() {
        if ("AzureOpenAI".equals(config.getType())) {
            // Azure OpenAI 不支持列出模型，返回配置的模型
            return Mono.just(getConfiguredModels());
        }

        String url = config.getBaseUrl() + "/models";

        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .map(responseStr -> {
                    // 解析响应并转换为我们的 ModelInfo 列表
                    // 这里简化处理，实际应该解析 JSON
                    List<ModelInfo> models = Collections.emptyList();
                    return models;
                })
                .timeout(webClientProperties.getTimeout().getRead()) // 使用配置的超时
                .onErrorResume(Exception.class, e -> {
                    log.warn("Failed to list models, returning configured models: {}", e.getMessage());
                    return Mono.just(getConfiguredModels());
                });
    }

    @Override
    public String getProviderName() {
        return config.getName();
    }

    /**
     * 构建聊天完成API的URL
     */
    private String buildChatCompletionUrl() {
        String baseUrl = config.getBaseUrl();
        
        if ("AzureOpenAI".equals(config.getType()) && config.getDeploymentName() != null) {
            // Azure OpenAI格式: /deployments/{deployment-name}/chat/completions
            return baseUrl + "/deployments/" + config.getDeploymentName() + 
                   "/chat/completions?api-version=2024-02-15-preview";
        } else {
            // 标准OpenAI格式: /chat/completions
            return baseUrl + "/chat/completions";
        }
    }

    /**
     * 获取配置文件中定义的模型列表
     */
    private List<ModelInfo> getConfiguredModels() {
        if (config.getModels() == null) {
            return Collections.emptyList();
        }

        return config.getModels().stream()
                .map(model -> {
                    ModelInfo info = new ModelInfo();
                    info.setId(model.getName());
                    info.setObject("model");
                    info.setOwnedBy(getProviderName());
                    return info;
                })
                .toList();
    }

    /**
     * 处理WebClient异常
     */
    private ProviderException handleWebClientError(WebClientResponseException ex) {
        String errorMessage = String.format("OpenAI API error: %d %s", 
                ex.getStatusCode().value(), ex.getStatusText());

        ex.getResponseBodyAsString();
        if (!ex.getResponseBodyAsString().isEmpty()) {
            errorMessage += " - " + ex.getResponseBodyAsString();
        }
        
        return new ProviderException(errorMessage, ex);
    }
}
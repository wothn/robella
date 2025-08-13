package org.elmo.robella.adapter.openai;

import lombok.extern.slf4j.Slf4j;
import org.elmo.robella.adapter.AIProviderAdapter;
import org.elmo.robella.config.ProviderConfig;
import org.elmo.robella.config.ProviderType;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.io.IOException;

import reactor.util.retry.Retry;

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
        if (!(request instanceof ChatCompletionRequest openaiRequest)) {
            return Mono.error(new ProviderException("Invalid request type for OpenAIAdapter: " + (request == null ? "null" : request.getClass().getName())));
        }

        // 构建URL
        String url = buildChatCompletionUrl();

        // 发送请求
        if (log.isDebugEnabled()) {
            log.debug("[OpenAIAdapter] chatCompletion start provider={} model={} stream=false", getProviderName(), openaiRequest.getModel());
        }

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
                // 仅对非流式请求添加重试（流式重试可能造成重复 token）
                .retryWhen(Retry.backoff(webClientProperties.getRetry().getMaxAttempts(), webClientProperties.getRetry().getInitialDelay())
                        .maxBackoff(webClientProperties.getRetry().getMaxDelay())
                        .filter(this::isRetryable))
                .onErrorMap(WebClientResponseException.class, this::handleWebClientError)
                .onErrorMap(ex -> !(ex instanceof ProviderException), ex -> new ProviderException(
                        "OpenAI API call failed: " + ex.getMessage(), ex))
                .doOnSuccess(resp -> {
                    if (log.isDebugEnabled())
                        log.debug("[OpenAIAdapter] chatCompletion success provider={} model={}", getProviderName(), openaiRequest.getModel());
                })
                .doOnError(err -> log.debug("[OpenAIAdapter] chatCompletion error provider={} model={} msg={}", getProviderName(), openaiRequest.getModel(), err.toString()));
    }

    @Override
    public Flux<String> streamChatCompletion(Object request) {
        if (!(request instanceof ChatCompletionRequest originalReq)) {
            return Flux.error(new ProviderException("Invalid request type for OpenAIAdapter: " + (request == null ? "null" : request.getClass().getName())));
        }
        // 克隆请求对象，避免调用方复用实例被副作用修改
        ChatCompletionRequest openaiRequest = JsonUtils.fromJson(JsonUtils.toJson(originalReq), ChatCompletionRequest.class);
        if (openaiRequest == null) {
            return Flux.error(new ProviderException("Failed to clone ChatCompletionRequest"));
        }
        
        // 强制开启流式模式，确保上游返回 SSE 格式
        openaiRequest.setStream(true);

        String url = buildChatCompletionUrl();

        // 流式请求通常需要更长的超时时间，使用读超时的5倍
        double multiplier = webClientProperties.getTimeout().getStreamReadMultiplier();
        if (multiplier <= 0) multiplier = 5.0;
        Duration streamTimeout = Duration.ofMillis((long) (webClientProperties.getTimeout().getRead().toMillis() * multiplier));

        if (log.isDebugEnabled()) {
            log.debug("[OpenAIAdapter] streamChatCompletion start provider={} model={} stream=true", getProviderName(), openaiRequest.getModel());
        }

        return webClient.post()
                .uri(url)
                .header(HttpHeaders.ACCEPT, "text/event-stream")
                .bodyValue(openaiRequest)
                .retrieve()
                .bodyToFlux(String.class)
                .doOnNext(raw -> {
                    if (log.isTraceEnabled()) {
                        log.trace("[OpenAIAdapter] raw stream fragment: {}", abbreviate(raw, 200));
                    }
                })
                .flatMap(this::parseSseRaw) // 统一解析
                .timeout(streamTimeout)
                .onErrorMap(WebClientResponseException.class, this::handleWebClientError)
                .onErrorMap(ex -> !(ex instanceof ProviderException), ex -> new ProviderException(
                        "OpenAI streaming API call failed: " + ex.getMessage(), ex))
                .doOnError(err -> log.debug("[OpenAIAdapter] streamChatCompletion error provider={} model={} msg={}", getProviderName(), openaiRequest.getModel(), err.toString()));
    }

    @Override
    public Mono<List<ModelInfo>> listModels() {
        if (config.getProviderType() == ProviderType.AzureOpenAI) {
            // Azure OpenAI 不支持列出模型，返回配置的模型
            return Mono.just(getConfiguredModels());
        }

        String url = config.getBaseUrl() + "/models";

        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .map(responseStr -> parseModelList(responseStr))
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

        if (config.getProviderType() == ProviderType.AzureOpenAI && config.getDeploymentName() != null) {
            // Azure OpenAI格式: /deployments/{deployment-name}/chat/completions
            String apiVersion = config.getApiVersion() != null ? config.getApiVersion() : "2024-02-15-preview";
            return baseUrl + "/deployments/" + config.getDeploymentName() +
                    "/chat/completions?api-version=" + apiVersion;
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
        String body = ex.getResponseBodyAsString();
        String errorMessage = String.format("OpenAI API error: %d %s%s",
                ex.getStatusCode().value(),
                ex.getStatusText(),
                (body == null || body.isEmpty()) ? "" : " - " + abbreviate(body, 200));
        return new ProviderException(errorMessage, ex);
    }

    private boolean isRetryable(Throwable t) {
        if (t instanceof ProviderException pe) {
            Throwable cause = pe.getCause();
            if (cause instanceof WebClientResponseException wex) {
                int status = wex.getStatusCode().value();
                return status >= 500 || status == 429; // 服务器错误与限流可重试
            }
            if (cause instanceof IOException || cause instanceof TimeoutException) return true;
        } else if (t instanceof WebClientResponseException wex) {
            int status = wex.getStatusCode().value();
            return status >= 500 || status == 429;
        } else if (t instanceof IOException || t instanceof TimeoutException) {
            return true;
        }
        return false;
    }

    private String abbreviate(String str, int max) {
        if (str == null || str.length() <= max) return str;
        return str.substring(0, max) + "...";
    }

    /**
     * 解析 OpenAI /v1/models 响应
     */
    private List<ModelInfo> parseModelList(String json) {
        if (json == null || json.isEmpty()) return getConfiguredModels();
        try {
            // OpenAI 模型列表结构: { "data": [ {"id":"gpt-4o", "owned_by":"openai"}, ... ] }
            var node = JsonUtils.fromJson(json, com.fasterxml.jackson.databind.JsonNode.class);
            if (node == null || !node.has("data")) return getConfiguredModels();
            var data = node.get("data");
            List<ModelInfo> list = new java.util.ArrayList<>();
            data.forEach(n -> {
                if (n.has("id")) {
                    list.add(new ModelInfo(n.get("id").asText(), "model", n.has("owned_by") ? n.get("owned_by").asText() : getProviderName()));
                }
            });
            if (list.isEmpty()) return getConfiguredModels();
            return list;
        } catch (Exception e) {
            log.debug("Failed to parse model list JSON: {}", e.getMessage());
            return getConfiguredModels();
        }
    }

    /**
     * 解析 SSE 原始块，提取 data: 行；[DONE] 单独作为事件；返回纯 data 内容。
     * 
     * @param raw 包含SSE格式数据的原始字符串
     * @return 包含解析后的SSE数据行的Flux流，每个元素是data部分的内容或[DONE]标记
     */
    private Flux<String> parseSseRaw(String raw) {
        if (raw == null || raw.isEmpty()) return Flux.empty();
        
        // 回退处理：如果收到的是单个JSON响应而不是SSE格式，直接返回
        String trimmed = raw.trim();
        if (trimmed.startsWith("{") && trimmed.contains("\"choices\"")) {
            if (log.isTraceEnabled()) {
                log.trace("[OpenAIAdapter] Detected non-SSE JSON response, using fallback");
            }
            return Flux.just(trimmed);
        }
        
        // 按事件块分割
        String[] blocks = raw.split("\r?\n\r?\n");
        return Flux.fromArray(blocks)
                .flatMap(this::parseEventBlock);
    }
    
    /**
     * 解析单个 SSE 事件块
     */
    private Flux<String> parseEventBlock(String block) {
        if (block == null || block.trim().isEmpty()) return Flux.empty();
        
        String[] lines = block.split("\r?\n");
        List<String> dataLines = new ArrayList<>();
        boolean foundDone = false;
        
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith(":")) continue; // 忽略空行和注释行
            
            if (trimmed.startsWith("data:")) {
                String content = trimmed.substring(5).trim();
                if ("[DONE]".equals(content)) {
                    foundDone = true;
                    // 继续处理其他data行，最后再发送DONE
                } else if (!content.isEmpty()) {
                    dataLines.add(content);
                }
            }
        }
        
        // 先发送数据行，再发送DONE标记
        Flux<String> dataFlux = dataLines.isEmpty() ? Flux.empty() : Flux.fromIterable(dataLines);
        Flux<String> doneFlux = foundDone ? Flux.just("[DONE]") : Flux.empty();
        
        return Flux.concat(dataFlux, doneFlux);
    }
}
package org.elmo.robella.adapter.openai;

import lombok.extern.slf4j.Slf4j;
import org.elmo.robella.adapter.AIProviderAdapter;
import org.elmo.robella.config.ProviderConfig;
import org.elmo.robella.config.ProviderType;
import org.elmo.robella.config.WebClientProperties;
import org.elmo.robella.exception.AuthenticationException;
import org.elmo.robella.exception.ProviderException;
import org.elmo.robella.exception.QuotaExceededException;
import org.elmo.robella.exception.RateLimitException;
import org.elmo.robella.model.openai.core.ChatCompletionRequest;
import org.elmo.robella.model.openai.core.ChatCompletionResponse;
import org.elmo.robella.model.openai.model.ModelInfo;
import org.elmo.robella.model.openai.stream.ChatCompletionChunk;
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
import java.util.regex.Pattern;


@Slf4j
public class OpenAIAdapter implements AIProviderAdapter {

    private static final String SSE_DATA_PREFIX = "data: ";
    private static final String SSE_DONE_MARKER = "[DONE]";
    private static final Pattern SSE_LINE_PATTERN = Pattern.compile("(?:^|\\n)data: (.*)(?:\\n|$)");

    private final ProviderConfig.Provider config;
    private final WebClient webClient;
    private final WebClientProperties webClientProperties;

    public OpenAIAdapter(ProviderConfig.Provider config, WebClient webClient, WebClientProperties webClientProperties) {
        this.config = config;
        this.webClient = webClient.mutate()
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + config.getApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.USER_AGENT, "Robella")
                .build();
        this.webClientProperties = webClientProperties;
    }

    @Override
    public Mono<ChatCompletionResponse> chatCompletion(Object request) {
        if (!(request instanceof ChatCompletionRequest openaiRequest)) {
            return Mono.error(new ProviderException("Invalid request type for OpenAIAdapter: " + (request == null ? "null" : request.getClass().getName())));
        }

        // 构建URL
        String url = buildChatCompletionsUrl();

        // 发送请求
        if (log.isDebugEnabled()) {
            log.debug("[OpenAIAdapter] chatCompletion start provider={} model={} stream=false", config.getName(), openaiRequest.getModel());
        }

        return webClient.post()
                .uri(url)
                .bodyValue(openaiRequest)
                .retrieve()
                .bodyToMono(String.class)  // 先获取为字符串
                .mapNotNull(responseStr -> {
                    try {
                        // 手动反序列化JSON字符串为OpenAIChatResponse对象
                        return JsonUtils.fromJson(responseStr, ChatCompletionResponse.class);
                    } catch (Exception e) {
                        throw new ProviderException("Failed to deserialize response: " + e.getMessage(), e);
                    }
                })
                .timeout(webClientProperties.getTimeout().getRead())  // 使用配置的读超时
                .onErrorMap(ex -> mapToProviderException(ex, "OpenAI API call"))
                .doOnSuccess(resp -> {
                    if (log.isDebugEnabled())
                        log.debug("[OpenAIAdapter] chatCompletion success provider={} model={}", config.getName(), openaiRequest.getModel());
                })
                .doOnError(err -> log.debug("[OpenAIAdapter] chatCompletion error provider={} model={} msg={}", config.getName(), openaiRequest.getModel(), err.toString()));
    }

    @Override
    public Flux<ChatCompletionChunk> streamChatCompletion(Object request) {
        if (!(request instanceof ChatCompletionRequest openaiRequest)) {
            return Flux.error(new ProviderException("Invalid request type for OpenAIAdapter: " + (request == null ? "null" : request.getClass().getName())));
        }

        // 构建URL
        String url = buildChatCompletionsUrl();

        // 流式请求通常需要更长的超时时间，使用读超时的5倍
        double multiplier = webClientProperties.getTimeout().getStreamReadMultiplier();
        if (multiplier <= 0) multiplier = 5.0;
        Duration streamTimeout = Duration.ofMillis((long) (webClientProperties.getTimeout().getRead().toMillis() * multiplier));

        if (log.isDebugEnabled()) {
            log.debug("[OpenAIAdapter] streamChatCompletion start provider={} model={} stream=true", config.getName(), openaiRequest.getModel());
        }

        return webClient.post()
                .uri(url)
                .header(HttpHeaders.ACCEPT, "text/event-stream")
                .bodyValue(openaiRequest)
                .retrieve()
                .bodyToFlux(String.class)
                .doOnNext(raw -> {
                    if (log.isTraceEnabled()) {
                        log.trace("[OpenAIAdapter] raw stream fragment: {}", raw != null && raw.length() > 200 ? raw.substring(0, 200) + "..." : raw);
                    }
                })
                .flatMap(raw -> {
                    List<ChatCompletionChunk> parsed = parseStreamRaw(raw);
                    return parsed.isEmpty() ? Flux.empty() : Flux.fromIterable(parsed);
                })
                .timeout(streamTimeout)
                .onErrorMap(WebClientResponseException.class, this::handleApiError)
                .onErrorMap(ex -> mapToProviderException(ex, "OpenAI streaming API call"))
                .doOnError(err -> log.debug("[OpenAIAdapter] streamChatCompletion error provider={} model={} msg={}", config.getName(), openaiRequest.getModel(), err.toString()));
    }

    @Override
    public Mono<List<ModelInfo>> listModels() {
        if (config.getProviderType() == ProviderType.AzureOpenAI) {
            // Azure OpenAI 不支持列出模型，返回配置的模型
            return Mono.just(getConfiguredModelInfos());
        }

        String url = config.getBaseUrl() + "/models";

        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .map(responseStr -> {
                    if (responseStr == null || responseStr.isEmpty()) {
                        return getConfiguredModelInfos();
                    }

                    try {
                        // OpenAI 模型列表结构: { "data": [ {"id":"gpt-4o", "owned_by":"openai"}, ... ] }
                        var node = JsonUtils.fromJson(responseStr, com.fasterxml.jackson.databind.JsonNode.class);
                        if (node == null || !node.has("data")) {
                            return getConfiguredModelInfos();
                        }

                        var data = node.get("data");
                        List<ModelInfo> list = new java.util.ArrayList<>();
                        data.forEach(n -> {
                            if (n.has("id")) {
                                list.add(new ModelInfo(n.get("id").asText(), "model", n.has("owned_by") ? n.get("owned_by").asText() : config.getName()));
                            }
                        });

                        if (list.isEmpty()) {
                            return getConfiguredModelInfos();
                        }
                        return list;
                    } catch (Exception e) {
                        log.debug("Failed to parse model list JSON: {}", e.getMessage());
                        return getConfiguredModelInfos();
                    }
                })
                .timeout(webClientProperties.getTimeout().getRead()) // 使用配置的超时
                .onErrorResume(Exception.class, e -> {
                    log.warn("Failed to list models, returning configured models: {}", e.getMessage());
                    return Mono.just(getConfiguredModelInfos());
                });
    }

    @Override
    public String getProviderName() {
        return config.getName();
    }

    // ===================== 私有辅助方法 =====================

    /**
     * 统一的错误处理方法，将WebClientResponseException和其他异常转换为ProviderException
     * @param operationType 操作类型，用于生成错误消息
     * @return 错误映射函数
     */
    private ProviderException mapToProviderException(Throwable ex, String operationType) {
        if (ex instanceof WebClientResponseException webEx) {
            String body = webEx.getResponseBodyAsString();
            String errorMessage = String.format("OpenAI API error: %d %s%s",
                    webEx.getStatusCode().value(),
                    webEx.getStatusText(),
                    body.isEmpty() ? "" : " - " + (body.length() > 200 ? body.substring(0, 200) + "..." : body));
            return new ProviderException(errorMessage, ex);
        }
        
        if (ex instanceof ProviderException providerEx) {
            return providerEx;
        }
        
        return new ProviderException(operationType + " failed: " + ex.getMessage(), ex);
    }

    private String buildChatCompletionsUrl() {
        String baseUrl = config.getBaseUrl();
        if (config.getProviderType() == ProviderType.AzureOpenAI && config.getDeploymentName() != null) {
            String apiVersion = config.getApiVersion() != null ? config.getApiVersion() : "2024-02-15-preview";
            return baseUrl + "/deployments/" + config.getDeploymentName() + "/chat/completions?api-version=" + apiVersion;
        }
        return baseUrl + "/chat/completions";
    }

    private ProviderException handleApiError(WebClientResponseException ex) {
        String body = ex.getResponseBodyAsString();
        int status = ex.getStatusCode().value();

        String errorMessage = String.format("API error: %d %s%s",
                status,
                ex.getStatusText(),
                body.isEmpty() ? "" : " - " + (body.length() > 200 ? body.substring(0, 200) + "..." : body));

        return switch (status) {
            case 401 -> new AuthenticationException("Invalid API key or authentication failed", ex);
            case 402 -> new AuthenticationException("Access forbidden - check permissions", ex);
            case 429 -> {
                if (body.contains("quota")) {
                    yield new QuotaExceededException("API quota exceeded", ex);
                } else {
                    yield new RateLimitException("Rate limit exceeded", ex);
                }
            }
            case 400 -> new ProviderException("Bad request: " +
                    (!body.isEmpty() ? body : "Invalid request parameters"), ex);
            case 404 -> new ProviderException("Model or endpoint not found", ex);
            case 422 -> new ProviderException("Unprocessable entity: " +
                    (!body.isEmpty() ? body : "Invalid request format"), ex);
            default -> new ProviderException(errorMessage, ex);
        };
    }




    private List<ModelInfo> getConfiguredModelInfos() {
        if (config.getModels() == null) return Collections.emptyList();
        return config.getModels().stream().map(m -> {
            ModelInfo info = new ModelInfo();
            info.setId(m.getName());
            info.setObject("model");
            info.setOwnedBy(config.getName());
            return info;
        }).toList();
    }

    /**
     * 解析流数据
     * 兼容两种格式：直接JSON和SSE格式
     * @param raw 流数据片段
     * @return 解析后的数据块
     */
    private List<ChatCompletionChunk> parseStreamRaw(String raw) {
        if (raw == null || raw.isEmpty()) {
            return Collections.emptyList();
        }

        String trimmed = raw.trim();

        // 检查结束标记
        if (SSE_DONE_MARKER.equals(trimmed)) {
            if (log.isTraceEnabled()) {
                log.trace("[OpenAIAdapter] Received stream completion marker");
            }
            return Collections.emptyList();
        }

        List<ChatCompletionChunk> chunks = new ArrayList<>();
        String jsonData;

        // 判断是否为SSE格式
        if (trimmed.startsWith(SSE_DATA_PREFIX)) {
            jsonData = trimmed.substring(SSE_DATA_PREFIX.length()).trim();
            if (SSE_DONE_MARKER.equals(jsonData)) {
                return Collections.emptyList();
            }
        } else {
            // 直接是JSON格式
            jsonData = trimmed;
        }

        // 尝试解析JSON
        try {
            ChatCompletionChunk chunk = JsonUtils.fromJson(jsonData, ChatCompletionChunk.class);
            if (chunk != null) {
                chunks.add(chunk);
                if (log.isTraceEnabled()) {
                    log.trace("[OpenAIAdapter] Received stream chunk: {}", chunk);
                }
            }
        } catch (Exception e) {
            if (log.isTraceEnabled()) {
                log.trace("[OpenAIAdapter] Failed to parse stream chunk: {} - Data: {}",
                        e.getMessage(), jsonData.length() > 100 ? jsonData.substring(0, 100) + "..." : jsonData);
            }
        }

        return chunks;
    }


}
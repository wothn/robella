package org.elmo.robella.adapter.anthropic;

import lombok.extern.slf4j.Slf4j;
import org.elmo.robella.adapter.AIProviderAdapter;
import org.elmo.robella.config.ProviderConfig;
import org.elmo.robella.config.WebClientProperties;
import org.elmo.robella.exception.AuthenticationException;
import org.elmo.robella.exception.ProviderException;
import org.elmo.robella.exception.QuotaExceededException;
import org.elmo.robella.exception.RateLimitException;
import org.elmo.robella.model.anthropic.core.AnthropicChatRequest;
import org.elmo.robella.model.anthropic.core.AnthropicMessage;
import org.elmo.robella.model.anthropic.stream.AnthropicStreamEvent;
import org.elmo.robella.model.openai.model.ModelInfo;
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


/**
 * Anthropic Messages API 适配器
 */
@Slf4j
public class AnthropicAdapter implements AIProviderAdapter {

    private static final String SSE_DATA_PREFIX = "data: ";
    private static final String SSE_EVENT_PREFIX = "event: ";

    private final ProviderConfig.Provider config;
    private final WebClient webClient;
    private final WebClientProperties webClientProperties;
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    public AnthropicAdapter(ProviderConfig.Provider config,
            WebClient baseClient,
            WebClientProperties webClientProperties) {
        this.config = config;
        this.webClient = baseClient.mutate()
                .defaultHeader("x-api-key", config.getApiKey())
                .defaultHeader("anthropic-version", ANTHROPIC_VERSION)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.USER_AGENT, "Robella")
                .build();
        this.webClientProperties = webClientProperties;
    }

    @Override
    public Mono<AnthropicMessage> chatCompletion(Object request) {
        if (!(request instanceof AnthropicChatRequest anthropicRequest)) {
            return Mono.error(new ProviderException("Invalid request type for AnthropicAdapter: "
                    + (request == null ? "null" : request.getClass().getName())));
        }

        // URL 末尾统一 /v1/messages
        String url = buildMessagesUrl();

        if (log.isDebugEnabled()) {
            log.debug("[AnthropicAdapter] chatCompletion start provider={} model={} stream=false", config.getName(),
                    anthropicRequest.getModel());
        }

        return webClient.post()
                .uri(url)
                .bodyValue(anthropicRequest)
                .retrieve()
                .bodyToMono(String.class)
                .mapNotNull(json -> {
                    try {
                        return JsonUtils.fromJson(json, AnthropicMessage.class);
                    } catch (Exception e) {
                        throw new ProviderException("Failed to deserialize Anthropic response: " + e.getMessage(), e);
                    }
                })
                .timeout(webClientProperties.getTimeout().getRead())
                .onErrorMap(ex -> mapToProviderException(ex, "Anthropic API call"))
                .doOnSuccess(resp -> {
                    if (log.isDebugEnabled())
                        log.debug("[AnthropicAdapter] chatCompletion success provider={} model={}", config.getName(),
                                anthropicRequest.getModel());
                })
                .doOnError(err -> log.debug("[AnthropicAdapter] chatCompletion error provider={} model={} msg={}",
                        config.getName(), anthropicRequest.getModel(), err.toString()));
    }

    @Override
    public Flux<AnthropicStreamEvent> streamChatCompletion(Object request) {
        if (!(request instanceof AnthropicChatRequest anthropicRequest)) {
            return Flux.error(new ProviderException("Invalid request type for AnthropicAdapter: "
                    + (request == null ? "null" : request.getClass().getName())));
        }

        // 强制流式开关
        if (anthropicRequest.getStream() == null || !anthropicRequest.getStream()) {
            anthropicRequest.setStream(true);
        }

        String url = buildMessagesUrl();

        double multiplier = webClientProperties.getTimeout().getStreamReadMultiplier();
        if (multiplier <= 0)
            multiplier = 5.0;
        Duration streamTimeout = Duration
                .ofMillis((long) (webClientProperties.getTimeout().getRead().toMillis() * multiplier));

        if (log.isDebugEnabled()) {
            log.debug("[AnthropicAdapter] streamChatCompletion start provider={} model={} stream=true",
                    config.getName(), anthropicRequest.getModel());
        }

        return webClient.post()
                .uri(url)
                .header(HttpHeaders.ACCEPT, "text/event-stream")
                .bodyValue(anthropicRequest)
                .retrieve()
                .bodyToFlux(String.class)
                .flatMap(raw -> {
                    if (log.isTraceEnabled()) {
                        log.trace("[AnthropicAdapter] raw stream fragment: {}",
                                raw != null && raw.length() > 200 ? raw.substring(0, 200) + "..." : raw);
                    }
                    List<AnthropicStreamEvent> events = parseStreamRaw(raw);
                    return events.isEmpty() ? Flux.empty() : Flux.fromIterable(events);
                })
                .timeout(streamTimeout)
                .onErrorMap(WebClientResponseException.class, this::handleApiError)
                .onErrorMap(ex -> mapToProviderException(ex, "Anthropic streaming API call"))
                .doOnError(err -> log.debug("[AnthropicAdapter] streamChatCompletion error provider={} model={} msg={}",
                        config.getName(), anthropicRequest.getModel(), err.toString()));
    }

    @Override
    public Mono<List<ModelInfo>> listModels() {
        // Anthropic 暂无公开列出模型端点，返回配置模型
        return Mono.just(getConfiguredModelInfos());
    }

    @Override
    public String getProviderName() {
        return config.getName();
    }

    // ==== 辅助方法 ====

    private String buildMessagesUrl() {
        String base = config.getBaseUrl();
        if (base.endsWith("/"))
            base = base.substring(0, base.length() - 1);
        if (base.endsWith("/v1/messages"))
            return base; // 已经是完整路径
        if (base.endsWith("/v1"))
            return base + "/messages";
        return base + "/v1/messages";
    }

    private List<ModelInfo> getConfiguredModelInfos() {
        if (config.getModels() == null)
            return Collections.emptyList();
        return config.getModels().stream().map(m -> {
            ModelInfo info = new ModelInfo();
            info.setId(m.getName());
            info.setObject("model");
            info.setOwnedBy(config.getName());
            return info;
        }).toList();
    }

    private List<AnthropicStreamEvent> parseStreamRaw(String raw) {
        if (raw == null || raw.isEmpty())
            return Collections.emptyList();
        List<AnthropicStreamEvent> events = new ArrayList<>();

        String currentEventType = null;
        String currentData = null;

        String[] lines = raw.split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                // 空行表示事件结束，如果同时有 event 和 data 则处理它
                if (currentEventType != null && currentData != null) {
                    processEvent(events, currentEventType, currentData);
                }
                currentEventType = null;
                currentData = null;
                continue;
            }

            if (trimmed.startsWith(SSE_EVENT_PREFIX)) {
                currentEventType = trimmed.substring(SSE_EVENT_PREFIX.length()).trim();
            } else if (trimmed.startsWith(SSE_DATA_PREFIX)) {
                currentData = trimmed.substring(SSE_DATA_PREFIX.length()).trim();
            }

            // 如果同时获得了事件类型和数据，则处理该事件
            if (currentEventType != null && currentData != null) {
                processEvent(events, currentEventType, currentData);
                currentEventType = null;
                currentData = null;
            }
        }

        // 在结束时处理任何剩余的事件
        if (currentEventType != null && currentData != null) {
            processEvent(events, currentEventType, currentData);
        }

        return events;
    }

    private void processEvent(List<AnthropicStreamEvent> events, String eventType, String jsonData) {
        try {
            AnthropicStreamEvent event = JsonUtils.fromJson(jsonData, AnthropicStreamEvent.class);
            if (event != null) {
                // 确保事件类型与 JSON 中的 type 字段匹配
                if (event.getType() == null || !event.getType().equals(eventType)) {
                    if (log.isTraceEnabled()) {
                        log.trace("[AnthropicAdapter] Event type mismatch: SSE event={}, JSON type={}",
                                eventType, event.getType());
                    }
                }
                events.add(event);
            }
        } catch (Exception e) {
            if (log.isTraceEnabled()) {
                log.trace("[AnthropicAdapter] Failed to parse stream event: {} eventType={} data={}",
                        e.getMessage(), eventType,
                        jsonData.length() > 120 ? jsonData.substring(0, 120) + "..." : jsonData);
            }
        }
    }

    private ProviderException mapToProviderException(Throwable ex, String operation) {
        if (ex instanceof WebClientResponseException webEx) {
            return handleApiError(webEx);
        }
        if (ex instanceof ProviderException pe)
            return pe;
        return new ProviderException(operation + " failed: " + ex.getMessage(), ex);
    }

    private ProviderException handleApiError(WebClientResponseException ex) {
        String body = ex.getResponseBodyAsString();
        int status = ex.getStatusCode().value();
        String errorMessage = String.format("Anthropic API error: %d %s%s", status, ex.getStatusText(),
                body.isEmpty() ? "" : " - " + (body.length() > 200 ? body.substring(0, 200) + "..." : body));
        return switch (status) {
            case 401 -> new AuthenticationException("Invalid API key or authentication failed", ex);
            case 402 -> new AuthenticationException("Access forbidden - check permissions", ex);
            case 429 -> {
                if (body.contains("quota"))
                    yield new QuotaExceededException("API quota exceeded", ex);
                else
                    yield new RateLimitException("Rate limit exceeded", ex);
            }
            case 400 ->
                new ProviderException("Bad request: " + (!body.isEmpty() ? body : "Invalid request parameters"), ex);
            case 404 -> new ProviderException("Endpoint not found", ex);
            case 422 -> new ProviderException(
                    "Unprocessable entity: " + (!body.isEmpty() ? body : "Invalid request format"), ex);
            default -> new ProviderException(errorMessage, ex);
        };
    }

}

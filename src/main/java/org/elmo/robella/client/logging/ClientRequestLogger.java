package org.elmo.robella.client.logging;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elmo.robella.model.entity.RequestLog;
import org.elmo.robella.model.anthropic.core.AnthropicChatRequest;
import org.elmo.robella.model.anthropic.core.AnthropicMessage;
import org.elmo.robella.model.openai.core.ChatCompletionRequest;
import org.elmo.robella.model.openai.core.ChatCompletionResponse;
import org.elmo.robella.model.openai.stream.ChatCompletionChunk;
import org.elmo.robella.model.anthropic.stream.AnthropicStreamEvent;
import org.elmo.robella.model.anthropic.stream.AnthropicContentBlockDeltaEvent;
import org.elmo.robella.model.anthropic.stream.AnthropicMessageDeltaEvent;
import org.elmo.robella.service.RequestLogService;
import org.elmo.robella.service.TokenCountingService;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClientRequestLogger {

    private final RequestLogService requestLogService;
    private final TokenCountingService tokenCountingService;

    // 统一的请求状态跟踪
    private static final Map<String, RequestState> requestStateMap = new ConcurrentHashMap<>();

    /**
     * 开始记录请求日志 - OpenAI版本
     */
    public Mono<String> startRequest(boolean isStream, ChatCompletionRequest request) {
        return Mono.deferContextual(context -> {
            // 从context中获取requestId，如果不存在则生成新的
            String requestId = context.getOrDefault("requestId", UUID.randomUUID().toString());

            // 记录请求开始时间
            RequestState state = new RequestState();
            state.setRequestId(requestId);
            state.setStartTime(LocalDateTime.now());
            state.setStream(isStream);

            // 获取所有必要的context信息
            state.setUserId(context.getOrDefault("userId", null));
            state.setApiKeyId(context.getOrDefault("apiKeyId", null));
            state.setEndpointType(context.getOrDefault("endpointType", "openai"));
            state.setModelKey(context.getOrDefault("modelKey", null));
            state.setProviderId(context.getOrDefault("providerId", null));
            state.setEndpointType(context.getOrDefault("endpointType", "openai"));

            // 如果提供了request，计算prompt tokens
            if (request != null && request.getMessages() != null) {
                state.setVendorModelKey(request.getModel());
                int promptTokens = tokenCountingService.countOpenAIRequestToken(request, request.getModel());
                state.setPromptTokens(promptTokens);
                log.debug("OpenAI stream request {} calculated prompt tokens: {} for model {}",
                        requestId, promptTokens, request.getModel());
            }

            requestStateMap.put(requestId, state);
            return Mono.just(requestId);
        });
    }

    /**
     * 开始记录请求日志 - Anthropic版本
     */
    public Mono<String> startRequest(boolean isStream, AnthropicChatRequest request) {
        return Mono.deferContextual(context -> {
            // 从context中获取requestId，如果不存在则生成新的
            String requestId = context.getOrDefault("requestId", UUID.randomUUID().toString());

            // 记录请求开始时间
            RequestState state = new RequestState();
            state.setRequestId(requestId);
            state.setStartTime(LocalDateTime.now());
            state.setStream(isStream);

            // 获取所有必要的context信息
            state.setUserId(context.getOrDefault("userId", null));
            state.setApiKeyId(context.getOrDefault("apiKeyId", null));
            state.setEndpointType(context.getOrDefault("endpointType", "anthropic"));
            state.setModelKey(context.getOrDefault("modelKey", null));
            state.setProviderId(context.getOrDefault("providerId", null));
            state.setEndpointType(context.getOrDefault("endpointType", "anthropic"));

            // 如果提供了request，计算prompt tokens
            if (request != null && request.getMessages() != null) {
                state.setVendorModelKey(request.getModel());
                int promptTokens = tokenCountingService.countAnthropicRequestToken(request, request.getModel());
                state.setPromptTokens(promptTokens);
                log.debug("Anthropic stream request {} calculated prompt tokens: {} for model {}",
                        requestId, promptTokens, request.getModel());
            }

            requestStateMap.put(requestId, state);
            return Mono.just(requestId);
        });
    }

    /**
     * 记录成功的请求（非流式）
     */
    public Mono<Void> OpenAIlogSuccess(String requestId, ChatCompletionRequest request,
            ChatCompletionResponse response) {
        RequestLog baseLog = buildBaseRequestLog(requestId, request != null ? request.getModel() : null, false);
        if (baseLog == null) {
            return Mono.empty();
        }

        RequestLog.RequestLogBuilder data = baseLog.toBuilder();

        // 提取token信息
        if (response.getUsage() != null) {
            data.promptTokens(response.getUsage().getPromptTokens());
            data.completionTokens(response.getUsage().getCompletionTokens());
            data.totalTokens(response.getUsage().getTotalTokens());
            data.tokenSource("usage");
        } else {
            // 如果API没有返回usage，使用TokenCountingService计算prompt tokens
            if (request != null && request.getMessages() != null) {
                String modelName = request.getModel();
                int promptTokens = tokenCountingService.countOpenAIRequestToken(request, modelName);
                int completionTokens = tokenCountingService.countOpenAIResponseToken(response, modelName);
                data.promptTokens(promptTokens);
                data.completionTokens(completionTokens);
                data.tokenSource("jtokkit");
            }
        }

        return createSuccessLog(data.build()).then();
    }

    /**
     * 记录失败的请求
     */
    public Mono<Void> OpenAIlogFailure(String requestId, ChatCompletionRequest request,
            Throwable error) {
        RequestLog baseLog = buildBaseRequestLog(requestId, request != null ? request.getModel() : null, false);
        if (baseLog == null) {
            return Mono.empty();
        }

        return createFailureLog(baseLog).then();
    }

    /**
     * 记录Anthropic成功的请求
     */
    public Mono<Void> anthropicLogSuccess(String requestId, AnthropicChatRequest request,
            AnthropicMessage response) {
        RequestState state = requestStateMap.get(requestId);
        RequestLog baseLog = buildBaseRequestLog(requestId, state != null ? state.getVendorModelKey() : (request != null ? request.getModel() : null), state != null && state.isStream());
        if (baseLog == null) {
            return Mono.empty();
        }

        RequestLog.RequestLogBuilder data = baseLog.toBuilder();

        // 提取token信息
        if (response.getUsage() != null) {
            data.promptTokens(response.getUsage().getInputTokens());
            data.completionTokens(response.getUsage().getOutputTokens());
            data.totalTokens(response.getUsage().getInputTokens() + response.getUsage().getOutputTokens());
            data.tokenSource("usage");
        } else {
            // 如果API没有返回usage，使用TokenCountingService计算prompt tokens
            if (request != null && request.getMessages() != null) {
                String modelName = request.getModel();
                int promptTokens = tokenCountingService.countAnthropicRequestToken(request, modelName);
                int completionTokens = tokenCountingService.countAnthropicResponseToken(response, modelName);
                data.promptTokens(promptTokens);
                data.completionTokens(completionTokens);
                data.tokenSource("jtokkit");
            }
        }

        return createSuccessLog(data.build()).then();
    }

    /**
     * 记录Anthropic失败的请求
     */
    public Mono<Void> anthropicLogFailure(String requestId, AnthropicChatRequest request,
            Throwable error) {
        RequestState state = requestStateMap.get(requestId);
        RequestLog baseLog = buildBaseRequestLog(requestId, state != null ? state.getVendorModelKey() : (request != null ? request.getModel() : null), state != null && state.isStream());
        if (baseLog == null) {
            return Mono.empty();
        }

        return createFailureLog(baseLog).then();
    }

  
    /**
     * 记录流式chunk
     */
    public void logStreamChunk(String requestId, ChatCompletionChunk chunk) {
        RequestState state = requestStateMap.get(requestId);
        if (state == null || !state.isStream()) {
            return;
        }

        // 精确计算响应token数
        if (chunk.getChoices() != null && !chunk.getChoices().isEmpty()) {
            String modelKey = state.getModelKey();
            int tokens = tokenCountingService.countOpenAIStreamChunkToken(chunk, modelKey);
            state.incrementEstimatedResponseTokens(tokens);
        }

        // 记录第一个token的延迟
        if (state.getFirstTokenLatencyMs() == null) {
            long firstTokenLatency = Duration.between(state.getStartTime(), LocalDateTime.now()).toMillis();
            state.setFirstTokenLatencyMs((int) firstTokenLatency);
            log.info("Stream request {} first token received after {}ms", requestId, firstTokenLatency);
        }

        // 累积token统计
        if (chunk.getUsage() != null) {
            state.setPromptTokens(chunk.getUsage().getPromptTokens());
            state.setCompletionTokens(chunk.getUsage().getCompletionTokens());
            state.setTotalTokens(chunk.getUsage().getTotalTokens());
        }
    }

    /**
     * 记录Anthropic流式chunk
     */
    public void logStreamChunk(String requestId, AnthropicStreamEvent event) {
        RequestState state = requestStateMap.get(requestId);
        if (state == null || !state.isStream()) {
            return;
        }

        switch (event.getType()) {
            case "content_block_delta":
                // 精确计算响应token数和记录第一个token延迟
                if (event instanceof AnthropicContentBlockDeltaEvent) {
                    AnthropicContentBlockDeltaEvent deltaEvent = (AnthropicContentBlockDeltaEvent) event;
                    if (deltaEvent.getDelta() != null && deltaEvent.getDelta().getDeltaContent() != null) {
                        String modelKey = state.getModelKey();
                        int tokens = tokenCountingService.countAnthropicStreamEventToken(event, modelKey);
                        state.incrementEstimatedResponseTokens(tokens);

                        // 记录第一个token的延迟
                        if (state.getFirstTokenLatencyMs() == null) {
                            long firstTokenLatency = Duration.between(state.getStartTime(), LocalDateTime.now()).toMillis();
                            state.setFirstTokenLatencyMs((int) firstTokenLatency);
                            log.info("Anthropic stream request {} first token received after {}ms", requestId, firstTokenLatency);
                        }
                    }
                }
                break;

            case "message_delta":
                // 累积token统计
                if (event instanceof AnthropicMessageDeltaEvent) {
                    AnthropicMessageDeltaEvent deltaEvent = (AnthropicMessageDeltaEvent) event;
                    if (deltaEvent.getUsage() != null) {
                        state.setPromptTokens(deltaEvent.getUsage().getInputTokens());
                        state.setCompletionTokens(deltaEvent.getUsage().getOutputTokens());
                        state.setTotalTokens(
                                deltaEvent.getUsage().getInputTokens() + deltaEvent.getUsage().getOutputTokens());
                    }
                }
                break;

            case "message_start":
            case "message_stop":
            case "ping":
            case "error":
                // 这些事件不需要特殊处理
                break;
        }
    }

    public Mono<Void> completeStreamRequest(String requestId, ChatCompletionRequest request) {
        RequestState state = requestStateMap.remove(requestId);
        if (state == null) {
            log.warn("Stream state not found for request: {}", requestId);
            return Mono.empty();
        }

        log.debug("Completing stream request {} for user {}", requestId, state.getUserId());

        RequestLog.RequestLogBuilder data = RequestLog.builder();
        data.requestId(requestId);
        data.userId(state.getUserId());
        data.apiKeyId(state.getApiKeyId());
        data.modelKey(state.getModelKey());
        data.isStream(state.isStream());
        data.vendorModelKey(state.getVendorModelKey());
        data.providerId(state.getProviderId());
        data.endpointType(state.getEndpointType());

        Integer completionTokens = state.getCompletionTokens();
        if (completionTokens == null) {
            completionTokens = state.getEstimatedResponseTokens();
        }

        Integer totalTokens = state.getTotalTokens();
        if (totalTokens == null && state.getPromptTokens() != null) {
            totalTokens = state.getPromptTokens() + completionTokens;
        }

        data.promptTokens(state.getPromptTokens());
        data.completionTokens(completionTokens);
        data.totalTokens(totalTokens);
        data.firstTokenLatencyMs(state.getFirstTokenLatencyMs());
        data.tokenSource(completionTokens != null ? "usage" : "jtokkit");

        // 计算持续时间并添加到日志数据中
        int duration = calculateDurationMs(state.getStartTime());
        data.durationMs(duration);
        log.info("Stream request {} completed: total tokens: {}, duration: {}ms", requestId, totalTokens, duration);

        return createSuccessLog(data.build()).then();
    }

    /**
     * 完成Anthropic流式请求记录
     */
    public Mono<Void> completeStreamRequest(String requestId, AnthropicChatRequest request) {
        RequestState state = requestStateMap.remove(requestId);
        if (state == null)
            return Mono.empty();

        log.debug("Completing Anthropic stream request {} for user {}", requestId, state.getUserId());

        RequestLog.RequestLogBuilder data = RequestLog.builder();
        data.requestId(requestId);
        data.userId(state.getUserId());
        data.apiKeyId(state.getApiKeyId());
        data.modelKey(state.getModelKey());
        data.isStream(state.isStream());
        data.vendorModelKey(state.getVendorModelKey());
        data.providerId(state.getProviderId());

        // 优先使用API返回的token统计
        Integer completionTokens = state.getCompletionTokens();
        if (completionTokens == null) {
            completionTokens = state.getEstimatedResponseTokens();
        }

        Integer totalTokens = state.getTotalTokens();
        if (totalTokens == null && state.getPromptTokens() != null) {
            totalTokens = state.getPromptTokens() + completionTokens;
        }

        data.promptTokens(state.getPromptTokens());
        data.completionTokens(completionTokens);
        data.totalTokens(totalTokens);
        data.firstTokenLatencyMs(state.getFirstTokenLatencyMs());
        data.tokenSource(completionTokens != null ? "stream_accumulated" : "stream_jtokkit_calculated");

        // 计算持续时间并添加到日志数据中
        int duration = calculateDurationMs(state.getStartTime());
        data.durationMs(duration);
        log.info("Anthropic stream request {} completed: total tokens: {}, duration: {}ms",
                requestId, totalTokens, duration);

        return createSuccessLog(data.build()).then();
    }

    /**
     * 记录流式请求失败
     */
    public Mono<Void> failStreamRequest(String requestId, ChatCompletionRequest request, String endpointType,
            Throwable error) {
        RequestLog baseLog = buildBaseRequestLog(requestId, request != null ? request.getModel() : null, true);
        if (baseLog == null) {
            return Mono.empty();
        }

        RequestLog.RequestLogBuilder data = baseLog.toBuilder()
                .endpointType(endpointType);

        return createFailureLog(data.build()).then();
    }

    /**
     * 记录Anthropic流式请求失败
     */
    public Mono<Void> failStreamRequest(String requestId, AnthropicChatRequest request, String endpointType,
            Throwable error) {
        RequestLog baseLog = buildBaseRequestLog(requestId, request != null ? request.getModel() : null, true);
        if (baseLog == null) {
            return Mono.empty();
        }

        RequestLog.RequestLogBuilder data = baseLog.toBuilder()
                .endpointType(endpointType);

        return createFailureLog(data.build()).then();
    }

    /**
     * 计算持续时间（毫秒）
     */
    private Integer calculateDurationMs(LocalDateTime startTime) {
        return (int) java.time.Duration.between(startTime, LocalDateTime.now())
                .toMillis();
    }

    /**
     * 计算每秒token数
     */
    private BigDecimal calculateTokensPerSecond(Integer completionTokens, Integer durationMs) {
        if (completionTokens == null || durationMs == null || durationMs == 0) {
            return null;
        }

        double tokensPerSecond = (completionTokens.doubleValue() / durationMs) * 1000;
        return BigDecimal.valueOf(tokensPerSecond).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 从RequestState构建基础RequestLog数据
     */
    private RequestLog buildBaseRequestLog(String requestId, String vendorModelKey, boolean isStream) {
        RequestState state = requestStateMap.get(requestId);
        if (state == null) {
            log.warn("Request state not found for request: {}", requestId);
            return null;
        }

        return RequestLog.builder()
                .requestId(requestId)
                .userId(state.getUserId())
                .apiKeyId(state.getApiKeyId())
                .modelKey(state.getModelKey())
                .vendorModelKey(vendorModelKey)
                .providerId(state.getProviderId())
                .endpointType(state.getEndpointType())
                .isStream(isStream)
                .build();
    }

    /**
     * 创建成功的请求日志
     */
    public Mono<RequestLog> createSuccessLog(RequestLog requestLog) {
        RequestState state = requestStateMap.remove(requestLog.getRequestId());

        // 对于流式请求，durationMs 已经在 completeStreamRequest 中计算并设置
        // 对于非流式请求，需要在这里计算 durationMs
        Integer durationMs = requestLog.getDurationMs();
        if (durationMs == null) {
            LocalDateTime startTime = state != null ? state.getStartTime() : LocalDateTime.now();
            durationMs = calculateDurationMs(startTime);
        }

        BigDecimal inputCost = BigDecimal.ZERO;
        BigDecimal outputCost = BigDecimal.ZERO;
        BigDecimal totalCost = BigDecimal.ZERO;
        String currency = "USD";

        RequestLog.RequestLogBuilder builder = requestLog.toBuilder()
                .durationMs(durationMs)
                .inputCost(inputCost)
                .outputCost(outputCost)
                .totalCost(totalCost)
                .currency(currency)
                .tokensPerSecond(calculateTokensPerSecond(requestLog.getCompletionTokens(), durationMs))
                .isSuccess(true);

        return requestLogService.createRequestLog(builder.build());
    }

    /**
     * 创建失败的请求日志
     */
    public Mono<RequestLog> createFailureLog(RequestLog requestLog) {
        RequestState state = requestStateMap.remove(requestLog.getRequestId());
        LocalDateTime startTime = state != null ? state.getStartTime() : LocalDateTime.now();

        RequestLog.RequestLogBuilder builder = requestLog.toBuilder()
                .durationMs(calculateDurationMs(startTime))
                .isSuccess(false);

        return requestLogService.createRequestLog(builder.build());
    }

    // 统一的请求状态跟踪类
    @Data
    private static class RequestState {
        private String requestId;
        private String endpointType;
        private LocalDateTime startTime;
        private Integer firstTokenLatencyMs;
        private Integer promptTokens;
        private Integer completionTokens;
        private Integer totalTokens;
        private Integer estimatedResponseTokens = 0;
        private Long providerId;
        private Long userId;
        private Long apiKeyId;
        private String modelKey;
        private String vendorModelKey;
        private boolean stream;

        public void incrementEstimatedResponseTokens(int tokens) {
            this.estimatedResponseTokens += tokens;
        }
    }
}
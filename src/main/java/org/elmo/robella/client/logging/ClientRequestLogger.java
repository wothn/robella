package org.elmo.robella.client.logging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elmo.robella.model.entity.RequestLog;
import org.elmo.robella.model.entity.VendorModel;
import org.elmo.robella.model.internal.UnifiedChatRequest;
import org.elmo.robella.model.anthropic.core.AnthropicChatRequest;
import org.elmo.robella.model.anthropic.core.AnthropicMessage;
import org.elmo.robella.model.openai.core.ChatCompletionRequest;
import org.elmo.robella.model.openai.core.ChatCompletionResponse;
import org.elmo.robella.model.openai.stream.ChatCompletionChunk;
import org.elmo.robella.model.anthropic.stream.AnthropicStreamEvent;
import org.elmo.robella.model.anthropic.stream.AnthropicMessageStartEvent;
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
import reactor.core.publisher.Flux;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClientRequestLogger {

    private final RequestLogService requestLogService;
    private final TokenCountingService tokenCountingService;

    // 普通请求的时间跟踪
    private static final Map<String, LocalDateTime> requestStartTimeMap = new ConcurrentHashMap<>();

    // 流式请求的状态跟踪
    private static final Map<String, StreamRequestState> streamStateMap = new ConcurrentHashMap<>();

    /**
     * 开始记录请求日志
     */
    public String startRequest() {
        // 记录请求开始时间
        String requestId = UUID.randomUUID().toString();
        requestStartTimeMap.put(requestId, LocalDateTime.now());
        return requestId;
    }

    /**
     * 记录成功的请求（非流式）
     */
    public Mono<Void> OpenAIlogSuccess(String requestId, ChatCompletionRequest request,
            ChatCompletionResponse response) {
        return Mono.deferContextual(context -> {
            Long userId = context.getOrDefault("userId", null);
            Long apiKeyId = context.getOrDefault("apiKeyId", null);

            RequestLog.RequestLogBuilder data = RequestLog.builder();
            data.requestId(requestId);
            data.userId(userId);
            data.apiKeyId(apiKeyId);
            data.modelName(request != null ? request.getModel() : null);
            data.isStream(false);

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
        });
    }

    /**
     * 记录失败的请求
     */
    public Mono<Void> OpenAIlogFailure(String requestId, ChatCompletionRequest request,
            Throwable error) {
        return Mono.deferContextual(context -> {
            Long userId = context.getOrDefault("userId", null);
            Long apiKeyId = context.getOrDefault("apiKeyId", null);

            RequestLog.RequestLogBuilder data = RequestLog.builder();
            data.requestId(requestId);
            data.userId(userId);
            data.apiKeyId(apiKeyId);
            data.modelName(request != null ? request.getModel() : null);
            data.isStream(false);

            return createFailureLog(data.build()).then();
        });
    }

    /**
     * 记录Anthropic成功的请求
     */
    public Mono<Void> anthropicLogSuccess(String requestId, AnthropicChatRequest request,
            AnthropicMessage response) {
        return Mono.deferContextual(context -> {
            Long userId = context.getOrDefault("userId", null);
            Long apiKeyId = context.getOrDefault("apiKeyId", null);

            RequestLog.RequestLogBuilder data = RequestLog.builder();
            data.requestId(requestId);
            data.userId(userId);
            data.apiKeyId(apiKeyId);
            data.modelName(request != null ? request.getModel() : null);
            data.isStream(false);

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
        });
    }

    /**
     * 记录Anthropic失败的请求
     */
    public Mono<Void> anthropicLogFailure(String requestId, AnthropicChatRequest request,
            Throwable error) {
        return Mono.deferContextual(context -> {
            Long userId = context.getOrDefault("userId", null);
            Long apiKeyId = context.getOrDefault("apiKeyId", null);

            RequestLog.RequestLogBuilder data = RequestLog.builder();
            data.requestId(requestId);
            data.userId(userId);
            data.apiKeyId(apiKeyId);
            data.modelName(request != null ? request.getModel() : null);
            data.isStream(false);

            return createFailureLog(data.build()).then();
        });
    }

    /**
     * 开始记录流式请求（OpenAI格式）
     */
    public void startStreamRequest(String requestId, ChatCompletionRequest request) {
        StreamRequestState state = new StreamRequestState();
        state.setRequestId(requestId);
        state.setStartTime(LocalDateTime.now());
        if (request != null) {
            state.setVendorModelName(request.getModel());
            // 计算并设置prompt token
            if (request.getMessages() != null) {
                int promptTokens = tokenCountingService.countOpenAIRequestToken(request, request.getModel());
                state.setPromptTokens(promptTokens);
                log.debug("Stream request {} calculated prompt tokens: {} for model {}", 
                         requestId, promptTokens, request.getModel());
            }
        }

        streamStateMap.put(requestId, state);
    }

    /**
     * 开始记录流式请求（Anthropic格式）
     */
    public void startStreamRequest(String requestId, AnthropicChatRequest request) {
        StreamRequestState state = new StreamRequestState();
        state.setRequestId(requestId);
        state.setStartTime(LocalDateTime.now());
        if (request != null) {
            state.setVendorModelName(request.getModel());
            // 计算并设置prompt token
            if (request.getMessages() != null) {
                int promptTokens = tokenCountingService.countAnthropicRequestToken(request, request.getModel());
                state.setPromptTokens(promptTokens);
                log.debug("Anthropic stream request {} calculated prompt tokens: {} for model {}", 
                         requestId, promptTokens, request.getModel());
            }
        }

        streamStateMap.put(requestId, state);
    }

    /**
     * 记录流式chunk
     */
    public void logStreamChunk(String requestId, ChatCompletionChunk chunk) {
        StreamRequestState state = streamStateMap.get(requestId);
        if (state == null)
            return;

        // 精确计算响应token数
        if (chunk.getChoices() != null && !chunk.getChoices().isEmpty()) {
            String modelName = state.getModelName();
            int tokens = tokenCountingService.countOpenAIStreamChunkToken(chunk, modelName);
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
        StreamRequestState state = streamStateMap.get(requestId);
        if (state == null)
            return;

        switch (event.getType()) {
            case "content_block_delta":
                // 精确计算响应token数和记录第一个token延迟
                if (event instanceof AnthropicContentBlockDeltaEvent) {
                    AnthropicContentBlockDeltaEvent deltaEvent = (AnthropicContentBlockDeltaEvent) event;
                    if (deltaEvent.getDelta() != null && deltaEvent.getDelta().getDeltaContent() != null) {
                        String modelName = state.getModelName();
                        int tokens = tokenCountingService.countAnthropicStreamEventToken(event, modelName);
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

    /**
     * 完成流式请求记录
     */
    public void completeStreamRequest(String requestId, ChatCompletionRequest request) {
        StreamRequestState state = streamStateMap.remove(requestId);
        if (state == null)
            return;

        Mono.deferContextual(context -> {
            Long userId = context.getOrDefault("userId", null);
            Long apiKeyId = context.getOrDefault("apiKeyId", null);

            RequestLog.RequestLogBuilder data = RequestLog.builder();
            data.requestId(requestId);
            data.userId(userId);
            data.apiKeyId(apiKeyId);
            data.modelName(state.getModelName());
            data.isStream(true);
            data.vendorModelName(state.getVendorModelName());
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
            data.tokenSource(completionTokens != null ? "usage" : "jtokkit");

            long duration = Duration.between(state.getStartTime(), LocalDateTime.now()).toMillis();
            log.info("Stream request {} completed: total tokens: {}, duration: {}ms",
                    requestId, totalTokens, duration);

            return createSuccessLog(data.build()).then();
        }).subscribe();
    }

    /**
     * 完成Anthropic流式请求记录
     */
    public void completeStreamRequest(String requestId, AnthropicChatRequest request) {
        StreamRequestState state = streamStateMap.remove(requestId);
        if (state == null)
            return;

        Mono.deferContextual(context -> {
            Long userId = context.getOrDefault("userId", null);
            Long apiKeyId = context.getOrDefault("apiKeyId", null);

            RequestLog.RequestLogBuilder data = RequestLog.builder();
            data.requestId(requestId);
            data.userId(userId);
            data.apiKeyId(apiKeyId);
            data.modelName(state.getModelName());
            data.isStream(true);
            data.vendorModelName(state.getVendorModelName());
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

            long duration = Duration.between(state.getStartTime(), LocalDateTime.now()).toMillis();
            log.info("Anthropic stream request {} completed: total tokens: {}, duration: {}ms",
                    requestId, totalTokens, duration);

            return createSuccessLog(data.build()).then();
        }).subscribe();
    }

    /**
     * 记录流式请求失败
     */
    public void failStreamRequest(String requestId, ChatCompletionRequest request, String endpointType,
            Throwable error) {
        StreamRequestState state = streamStateMap.remove(requestId);
        if (state == null)
            return;

        Mono.deferContextual(context -> {
            Long userId = context.getOrDefault("userId", null);
            Long apiKeyId = context.getOrDefault("apiKeyId", null);

            RequestLog.RequestLogBuilder data = RequestLog.builder();
            data.requestId(requestId);
            data.userId(userId);
            data.apiKeyId(apiKeyId);
            data.modelName(request != null ? request.getModel() : null);
            data.endpointType(endpointType);
            data.isStream(true);

            return createFailureLog(data.build()).then();
        }).subscribe();
    }

    /**
     * 记录Anthropic流式请求失败
     */
    public void failStreamRequest(String requestId, AnthropicChatRequest request, String endpointType,
            Throwable error) {
        StreamRequestState state = streamStateMap.remove(requestId);
        if (state == null)
            return;

        Mono.deferContextual(context -> {
            Long userId = context.getOrDefault("userId", null);
            Long apiKeyId = context.getOrDefault("apiKeyId", null);

            RequestLog.RequestLogBuilder data = RequestLog.builder();
            data.requestId(requestId);
            data.userId(userId);
            data.apiKeyId(apiKeyId);
            data.modelName(request != null ? request.getModel() : null);
            data.endpointType(endpointType);
            data.isStream(true);

            return createFailureLog(data.build()).then();
        }).subscribe();
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
    private BigDecimal calculateTokensPerSecond(Integer totalTokens, Integer durationMs) {
        if (totalTokens == null || durationMs == null || durationMs == 0) {
            return null;
        }

        double tokensPerSecond = (totalTokens.doubleValue() / durationMs) * 1000;
        return BigDecimal.valueOf(tokensPerSecond).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 创建成功的请求日志
     */
    public Mono<RequestLog> createSuccessLog(RequestLog requestLog) {
        LocalDateTime startTime = requestStartTimeMap.remove(requestLog.getRequestId());
        if (startTime == null) {
            startTime = LocalDateTime.now();
        }

        // 计算总持续时间
        Integer durationMs = calculateDurationMs(startTime);

        // 注意：由于RequestLog没有VendorModel字段，我们需要通过其他方式获取成本信息
        // 这里简化处理，实际项目中可能需要从数据库获取VendorModel
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
        LocalDateTime startTime = requestStartTimeMap.remove(requestLog.getRequestId());
        if (startTime == null) {
            startTime = LocalDateTime.now();
        }

        RequestLog.RequestLogBuilder builder = requestLog.toBuilder()
                .durationMs(calculateDurationMs(startTime))
                .isSuccess(false);

        return requestLogService.createRequestLog(builder.build());
    }

    // 流式请求状态跟踪类
    @lombok.Data
    private static class StreamRequestState {
        private String requestId;
        private String endpointType;
        private LocalDateTime startTime;
        private Integer firstTokenLatencyMs;
        private Integer promptTokens;
        private Integer completionTokens;
        private Integer totalTokens;
        private Integer estimatedResponseTokens = 0;
        private String modelName;
        private String vendorModelName;
        private Long providerId;

        public void incrementEstimatedResponseTokens(int tokens) {
            this.estimatedResponseTokens += tokens;
        }
    }
}
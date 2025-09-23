package org.elmo.robella.client.logging;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elmo.robella.model.entity.RequestLog;
import org.elmo.robella.context.RequestContextHolder;
import org.elmo.robella.model.anthropic.core.AnthropicChatRequest;
import org.elmo.robella.model.anthropic.core.AnthropicMessage;
import org.elmo.robella.model.openai.core.ChatCompletionRequest;
import org.elmo.robella.model.openai.core.ChatCompletionResponse;
import org.elmo.robella.model.openai.core.Usage;
import org.elmo.robella.model.openai.stream.ChatCompletionChunk;
import org.elmo.robella.model.anthropic.stream.AnthropicStreamEvent;
import org.elmo.robella.model.anthropic.stream.AnthropicContentBlockDeltaEvent;
import org.elmo.robella.model.anthropic.stream.AnthropicMessageDeltaEvent;
import org.elmo.robella.service.BillingService;
import org.elmo.robella.service.RequestLogService;
import org.elmo.robella.service.TokenCountingService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class ClientRequestLogger {

    private final RequestLogService requestLogService;
    private final TokenCountingService tokenCountingService;
    private final BillingService billingService;

    // Token策略接口
    public interface TokenStrategy<T, R, S> {
        int countRequestToken(T request, String model);
        int countResponseToken(R response, String model);
        int countStreamToken(S streamEvent, String model);
        String extractModelName(T request);
        void processUsage(S streamEvent, LogState state);
        boolean shouldLogStreamToken(S streamEvent);
    }

    // OpenAI Token策略实现
    private static class OpenAITokenStrategy implements TokenStrategy<ChatCompletionRequest, ChatCompletionResponse, ChatCompletionChunk> {
        private final TokenCountingService tokenCountingService;

        public OpenAITokenStrategy(TokenCountingService tokenCountingService) {
            this.tokenCountingService = tokenCountingService;
        }

        @Override
        public int countRequestToken(ChatCompletionRequest request, String model) {
            return tokenCountingService.countOpenAIRequestToken(request, model);
        }

        @Override
        public int countResponseToken(ChatCompletionResponse response, String model) {
            return tokenCountingService.countOpenAIResponseToken(response, model);
        }

        @Override
        public int countStreamToken(ChatCompletionChunk chunk, String model) {
            return tokenCountingService.countOpenAIStreamChunkToken(chunk, model);
        }

        @Override
        public String extractModelName(ChatCompletionRequest request) {
            return request.getModel();
        }

        @Override
        public void processUsage(ChatCompletionChunk chunk, LogState state) {
            if (chunk.getUsage() != null) {
                state.setUsage(chunk.getUsage());
            }
        }

        @Override
        public boolean shouldLogStreamToken(ChatCompletionChunk chunk) {
            return chunk.getChoices() != null && !chunk.getChoices().isEmpty();
        }
    }

    // Anthropic Token策略实现
    private static class AnthropicTokenStrategy implements TokenStrategy<AnthropicChatRequest, AnthropicMessage, AnthropicStreamEvent> {
        private final TokenCountingService tokenCountingService;

        public AnthropicTokenStrategy(TokenCountingService tokenCountingService) {
            this.tokenCountingService = tokenCountingService;
        }

        @Override
        public int countRequestToken(AnthropicChatRequest request, String model) {
            return tokenCountingService.countAnthropicRequestToken(request, model);
        }

        @Override
        public int countResponseToken(AnthropicMessage response, String model) {
            return tokenCountingService.countAnthropicResponseToken(response, model);
        }

        @Override
        public int countStreamToken(AnthropicStreamEvent event, String model) {
            return tokenCountingService.countAnthropicStreamEventToken(event, model);
        }

        @Override
        public String extractModelName(AnthropicChatRequest request) {
            return request.getModel();
        }

        @Override
        public void processUsage(AnthropicStreamEvent event, LogState state) {
            if ("message_delta".equals(event.getType()) && event instanceof AnthropicMessageDeltaEvent deltaEvent) {
                if (deltaEvent.getUsage() != null) {
                    Usage usage = new Usage();
                    usage.setPromptTokens(deltaEvent.getUsage().getInputTokens());
                    usage.setCompletionTokens(deltaEvent.getUsage().getOutputTokens());
                    if (deltaEvent.getUsage().getInputTokens() != null && deltaEvent.getUsage().getOutputTokens() != null) {
                        usage.setTotalTokens(deltaEvent.getUsage().getInputTokens() + deltaEvent.getUsage().getOutputTokens());
                    }
                    state.setUsage(usage);
                }
            }
        }

        @Override
        public boolean shouldLogStreamToken(AnthropicStreamEvent event) {
            return "content_block_delta".equals(event.getType()) &&
                   event instanceof AnthropicContentBlockDeltaEvent deltaEvent &&
                   deltaEvent.getDelta() != null &&
                   deltaEvent.getDelta().getDeltaContent() != null;
        }
    }

    private final TokenStrategy<ChatCompletionRequest, ChatCompletionResponse, ChatCompletionChunk> openaiStrategy;
    private final TokenStrategy<AnthropicChatRequest, AnthropicMessage, AnthropicStreamEvent> anthropicStrategy;

    // 统一的请求状态跟踪
    private static final Map<String, LogState> requestStateMap = new ConcurrentHashMap<>();

    public ClientRequestLogger(RequestLogService requestLogService, TokenCountingService tokenCountingService, BillingService billingService) {
        this.requestLogService = requestLogService;
        this.tokenCountingService = tokenCountingService;
        this.billingService = billingService;
        this.openaiStrategy = new OpenAITokenStrategy(tokenCountingService);
        this.anthropicStrategy = new AnthropicTokenStrategy(tokenCountingService);
    }

    /**
     * 开始记录请求日志 - OpenAI版本
     */
    public String startRequest(boolean isStream, ChatCompletionRequest request, String requestId) {
        return startRequestGeneric(isStream, request, requestId, openaiStrategy);
    }

    /**
     * 开始记录请求日志 - Anthropic版本
     */
    public String startRequest(boolean isStream, AnthropicChatRequest request, String requestId) {
        return startRequestGeneric(isStream, request, requestId, anthropicStrategy);
    }

    /**
     * 通用开始记录请求日志方法
     */
    private <T> String startRequestGeneric(boolean isStream, T request, String requestId, TokenStrategy<T, ?, ?> strategy) {
        LogState state = new LogState();
        state.setRequestId(requestId);
        state.setStartTime(LocalDateTime.now());
        state.setStream(isStream);

        if (request != null) {
            String modelName = strategy.extractModelName(request);
            state.setVendorModelKey(modelName);

            if (hasMessages(request)) {
                int promptTokens = strategy.countRequestToken(request, modelName);
                Usage usage = new Usage();
                usage.setPromptTokens(promptTokens);
                state.setUsage(usage);
            }
        }

        requestStateMap.put(requestId, state);
        return requestId;
    }

    /**
     * 检查请求是否有消息
     */
    private boolean hasMessages(Object request) {
        if (request instanceof ChatCompletionRequest openAIRequest) {
            return openAIRequest.getMessages() != null;
        } else if (request instanceof AnthropicChatRequest anthropicRequest) {
            return anthropicRequest.getMessages() != null;
        }
        return false;
    }

    /**
     * 记录成功的请求（非流式）
     */
    public void logSuccess(String requestId, ChatCompletionRequest request,
            ChatCompletionResponse response) {
        logSuccessGeneric(requestId, request, response, openaiStrategy);
    }

    /**
     * 记录失败的请求
     */
    public void logFailure(String requestId, ChatCompletionRequest request,
            Throwable error) {
        logFailureGeneric(requestId, false);
    }

    /**
     * 记录Anthropic成功的请求
     */
    public void anthropicLogSuccess(String requestId, AnthropicChatRequest request,
            AnthropicMessage response) {
        logSuccessGeneric(requestId, request, response, anthropicStrategy);
    }

    /**
     * 记录Anthropic失败的请求
     */
    public void anthropicLogFailure(String requestId, AnthropicChatRequest request,
            Throwable error) {
        logFailureGeneric(requestId, false);
    }

    /**
     * 通用成功日志记录方法
     */
    private <T, R> void logSuccessGeneric(String requestId, T request, R response, TokenStrategy<T, R, ?> strategy) {
        LogState state = requestStateMap.get(requestId);
        boolean isStream = state != null && state.isStream();
        RequestLog baseLog = buildBaseRequestLog(requestId, isStream);
        if (baseLog == null) {
            return;
        }

        RequestLog.RequestLogBuilder data = baseLog.toBuilder();
        setTokenInfo(data, request, response, strategy);
        createSuccessLog(data.build());
    }

    /**
     * 通用失败日志记录方法
     */
    private void logFailureGeneric(String requestId, boolean isStream) {
        RequestLog baseLog = buildBaseRequestLog(requestId, isStream);
        if (baseLog == null) {
            return;
        }
        createFailureLog(baseLog);
    }

    /**
     * 设置token信息
     */
    private <T, R> void setTokenInfo(RequestLog.RequestLogBuilder data, T request, R response, TokenStrategy<T, R, ?> strategy) {
        if (hasUsage(response)) {
            setTokenInfoFromUsage(data, response);
            data.tokenSource("usage");
        } else if (hasMessages(request)) {
            setTokenInfoFromStrategy(data, request, response, strategy);
            data.tokenSource("jtokkit");
        }
    }

    /**
     * 检查响应是否有usage信息
     */
    private boolean hasUsage(Object response) {
        if (response instanceof ChatCompletionResponse openAIResponse) {
            return openAIResponse.getUsage() != null;
        } else if (response instanceof AnthropicMessage anthropicResponse) {
            return anthropicResponse.getUsage() != null;
        }
        return false;
    }

    /**
     * 从usage设置token信息
     */
    private void setTokenInfoFromUsage(RequestLog.RequestLogBuilder data, Object response) {
        if (response instanceof ChatCompletionResponse openAIResponse) {
            Usage usage = openAIResponse.getUsage();
            data.promptTokens(usage.getPromptTokens());
            data.completionTokens(usage.getCompletionTokens());
            data.totalTokens(usage.getTotalTokens());

            // 保存Usage信息到LogState用于后续计费
            String requestId = data.build().getRequestId();
            LogState state = requestStateMap.get(requestId);
            if (state != null) {
                state.setUsage(usage);
            }
        } else if (response instanceof AnthropicMessage anthropicResponse) {
            data.promptTokens(anthropicResponse.getUsage().getInputTokens());
            data.completionTokens(anthropicResponse.getUsage().getOutputTokens());
            data.totalTokens(anthropicResponse.getUsage().getInputTokens() + anthropicResponse.getUsage().getOutputTokens());
        }
    }

    /**
     * 从策略设置token信息
     */
    private <T, R> void setTokenInfoFromStrategy(RequestLog.RequestLogBuilder data, T request, R response, TokenStrategy<T, R, ?> strategy) {
        String modelName = strategy.extractModelName(request);
        int promptTokens = strategy.countRequestToken(request, modelName);
        int completionTokens = strategy.countResponseToken(response, modelName);
        data.promptTokens(promptTokens);
        data.completionTokens(completionTokens);
        data.totalTokens(promptTokens + completionTokens);
    }

    /**
     * 记录流式chunk
     */
    public void logStreamChunk(String requestId, ChatCompletionChunk chunk) {
        logStreamChunkGeneric(requestId, chunk, openaiStrategy);
    }

    /**
     * 记录Anthropic流式chunk
     */
    public void logStreamChunk(String requestId, AnthropicStreamEvent event) {
        logStreamChunkGeneric(requestId, event, anthropicStrategy);
    }

    /**
     * 通用流式chunk记录方法
     */
    private <T> void logStreamChunkGeneric(String requestId, T chunk, TokenStrategy<?, ?, T> strategy) {
        LogState state = requestStateMap.get(requestId);
        if (state == null || !state.isStream()) {
            return;
        }

        // 使用策略处理usage信息
        strategy.processUsage(chunk, state);

        // 如果应该记录token，则进行token计算和延迟记录
        if (strategy.shouldLogStreamToken(chunk)) {
            processStreamToken(state, requestId, chunk, strategy);
        }
    }

    /**
     * 处理流式token计算和延迟记录
     */
    private <T> void processStreamToken(LogState state, String requestId, T chunk, TokenStrategy<?, ?, T> strategy) {
        String modelKey = getModelKeyForChunk(state, chunk);
        int tokens = strategy.countStreamToken(chunk, modelKey);
        state.incrementEstimatedResponseTokens(tokens);

        // 记录第一个token的延迟
        if (state.getFirstTokenLatencyMs() == null) {
            long firstTokenLatency = Duration.between(state.getStartTime(), LocalDateTime.now()).toMillis();
            state.setFirstTokenLatencyMs((int) firstTokenLatency);
            log.info("Stream request {} first token received after {}ms", requestId, firstTokenLatency);
        }
    }

    /**
     * 获取chunk的modelKey
     */
    private <T> String getModelKeyForChunk(LogState state, T chunk) {
        if (chunk instanceof ChatCompletionChunk) {
            RequestContextHolder.RequestContext ctx = RequestContextHolder.getContext();
            return ctx.getModelKey();
        } else if (chunk instanceof AnthropicStreamEvent) {
            return state.getVendorModelKey();
        }
        return "unknown";
    }

    /**
     * 完成流式请求记录
     */
    public void completeStreamRequest(String requestId, ChatCompletionRequest request) {
        completeStreamRequestGeneric(requestId, false);
    }

    /**
     * 完成Anthropic流式请求记录
     */
    public void completeStreamRequest(String requestId, AnthropicChatRequest request) {
        completeStreamRequestGeneric(requestId, true);
    }

    /**
     * 通用完成流式请求记录方法
     */
    private void completeStreamRequestGeneric(String requestId, boolean isAnthropic) {
        LogState state = requestStateMap.remove(requestId);
        if (state == null) {
            log.warn("Stream state not found for request: {}", requestId);
            return;
        }

        RequestContextHolder.RequestContext ctx = RequestContextHolder.getContext();
        log.debug("Completing {} stream request {} for user {}",
                 isAnthropic ? "Anthropic" : "OpenAI", requestId, ctx.getUserId());

        RequestLog.RequestLogBuilder data = buildStreamRequestLog(requestId, state, ctx, isAnthropic);
        setStreamTokenInfo(data, state);

        int duration = calculateDurationMs(state.getStartTime());
        data.durationMs(duration);

        log.info("{} stream request {} duration: {}ms",
                isAnthropic ? "Anthropic" : "OpenAI", requestId, duration);

        createSuccessLog(data.build());
    }

    /**
     * 构建流式请求日志基础信息
     */
    private RequestLog.RequestLogBuilder buildStreamRequestLog(String requestId, LogState state,
                                                              RequestContextHolder.RequestContext ctx, boolean isAnthropic) {
        RequestLog.RequestLogBuilder data = RequestLog.builder();
        data.requestId(requestId);
        data.userId(ctx.getUserId());
        data.apiKeyId(ctx.getApiKeyId());
        data.modelKey(ctx.getModelKey());
        data.isStream(state.isStream());
        data.providerId(ctx.getProviderId());
        data.endpointType(ctx.getEndpointType());

        // 统一从RequestContext获取vendorModelKey
        if (ctx.getVendorModel() != null) {
            data.vendorModelKey(ctx.getVendorModel().getVendorModelKey());
        } else {
            data.vendorModelKey(state.getVendorModelKey());
        }

        return data;
    }

    /**
     * 设置流式请求的token信息
     */
    private void setStreamTokenInfo(RequestLog.RequestLogBuilder data, LogState state) {
        Integer completionTokens = null;
        Integer promptTokens = null;
        Integer totalTokens = null;

        if (state.getUsage() != null) {
            completionTokens = state.getUsage().getCompletionTokens();
            promptTokens = state.getUsage().getPromptTokens();
            totalTokens = state.getUsage().getTotalTokens();
        }

        if (completionTokens == null) {
            completionTokens = state.getEstimatedResponseTokens();
        }

        data.promptTokens(promptTokens);
        data.completionTokens(completionTokens);
        data.totalTokens(totalTokens);
        data.firstTokenLatencyMs(state.getFirstTokenLatencyMs());
        data.tokenSource(completionTokens != null ? "usage" : "jtokkit");
    }

    /**
     * 记录流式请求失败
     */
    public void failStreamRequest(String requestId, ChatCompletionRequest request, String endpointType,
            Throwable error) {
        failStreamRequestGeneric(requestId, endpointType);
    }

    /**
     * 记录Anthropic流式请求失败
     */
    public void failStreamRequest(String requestId, AnthropicChatRequest request, String endpointType,
            Throwable error) {
        failStreamRequestGeneric(requestId, endpointType);
    }

    /**
     * 通用流式请求失败记录方法
     */
    private void failStreamRequestGeneric(String requestId, String endpointType) {
        RequestLog baseLog = buildBaseRequestLog(requestId, true);
        if (baseLog == null) {
            return;
        }

        RequestLog.RequestLogBuilder data = baseLog.toBuilder()
                .endpointType(endpointType);

        createFailureLog(data.build());
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
    private RequestLog buildBaseRequestLog(String requestId, boolean isStream) {
        LogState state = requestStateMap.get(requestId);
        if (state == null) {
            log.warn("Request state not found for request: {}", requestId);
            return null;
        }

        RequestContextHolder.RequestContext ctx = RequestContextHolder.getContext();

        return RequestLog.builder()
                .requestId(requestId)
                .userId(ctx.getUserId())
                .apiKeyId(ctx.getApiKeyId())
                .modelKey(ctx.getModelKey())
                .vendorModelKey(ctx.getVendorModel().getVendorModelKey())
                .providerId(ctx.getProviderId())
                .endpointType(ctx.getEndpointType())
                .isStream(isStream)
                .build();
    }

    /**
     * 创建成功的请求日志
     */
    public RequestLog createSuccessLog(RequestLog requestLog) {
        LogState state = requestStateMap.remove(requestLog.getRequestId());

        // 对于流式请求，durationMs 已经在 completeStreamRequest 中计算并设置
        // 对于非流式请求，需要在这里计算 durationMs
        Integer durationMs = requestLog.getDurationMs();
        if (durationMs == null) {
            LocalDateTime startTime = state != null ? state.getStartTime() : LocalDateTime.now();
            durationMs = calculateDurationMs(startTime);
        }

        BillingService.BillingResult billingResult;

        // 如果有Usage信息（从state中获取），使用详细的Usage计算
        if (state != null && state.getUsage() != null) {
            billingResult = billingService.calculateCost(state.getUsage());
            log.debug("Billing calculated from Usage: cached={}, normal={}",
                     state.getUsage().getPromptCacheHitTokens(),
                     state.getUsage().getPromptCacheMissTokens());
        } else {
            // 没有Usage信息，使用token数量计算（可能是缓存响应或非OpenAI响应）
            Integer promptTokens = requestLog.getPromptTokens() != null ? requestLog.getPromptTokens() : 0;
            Integer completionTokens = requestLog.getCompletionTokens() != null ? requestLog.getCompletionTokens() : 0;
            Usage usage = new Usage();
            usage.setPromptTokens(promptTokens);
            usage.setCompletionTokens(completionTokens);
            usage.setTotalTokens(promptTokens + completionTokens);
            billingResult = billingService.calculateCost(usage);
        }

        RequestLog.RequestLogBuilder builder = requestLog.toBuilder()
                .durationMs(durationMs)
                .inputCost(billingResult.inputCost())
                .outputCost(billingResult.outputCost())
                .totalCost(billingResult.totalCost())
                .currency(billingResult.currency())
                .tokensPerSecond(calculateTokensPerSecond(requestLog.getCompletionTokens(), durationMs))
                .isSuccess(true);

        return requestLogService.createRequestLogSync(builder.build());
    }

    /**
     * 创建失败的请求日志
     */
    public RequestLog createFailureLog(RequestLog requestLog) {
        LogState state = requestStateMap.remove(requestLog.getRequestId());
        LocalDateTime startTime = state != null ? state.getStartTime() : LocalDateTime.now();

        RequestLog.RequestLogBuilder builder = requestLog.toBuilder()
                .durationMs(calculateDurationMs(startTime))
                .isSuccess(false);

        return requestLogService.createRequestLogSync(builder.build());
    }

    // 统一的请求状态跟踪类
    @Data
    private static class LogState {
        private String requestId;
        private LocalDateTime startTime;
        private Integer firstTokenLatencyMs;
        private Integer estimatedResponseTokens = 0;
        private String vendorModelKey;
        private boolean stream;
        private Usage usage; // 存储Usage信息用于详细计费

        public void incrementEstimatedResponseTokens(int tokens) {
            this.estimatedResponseTokens += tokens;
        }
    }
}
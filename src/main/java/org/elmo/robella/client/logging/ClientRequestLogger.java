package org.elmo.robella.client.logging;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elmo.robella.model.entity.RequestLog;
import org.elmo.robella.context.RequestContextHolder;
import org.elmo.robella.context.RequestContextHolder.RequestContext;
import org.elmo.robella.exception.InsufficientCreditsException;
import org.elmo.robella.model.anthropic.core.AnthropicChatRequest;
import org.elmo.robella.model.anthropic.core.AnthropicMessage;
import org.elmo.robella.model.openai.core.ChatCompletionRequest;
import org.elmo.robella.model.openai.core.ChatCompletionResponse;
import org.elmo.robella.model.openai.core.PromptTokensDetails;
import org.elmo.robella.model.openai.core.Usage;
import org.elmo.robella.model.openai.stream.ChatCompletionChunk;
import org.elmo.robella.model.anthropic.stream.AnthropicStreamEvent;
import org.elmo.robella.model.anthropic.stream.AnthropicMessageDeltaEvent;
import org.elmo.robella.model.anthropic.stream.AnthropicMessageStartEvent;
import org.elmo.robella.model.anthropic.stream.AnthropicContentBlockStartEvent;
import org.elmo.robella.service.RequestLogService;
import org.elmo.robella.service.UserService;
import org.elmo.robella.util.BillingUtils;
import org.elmo.robella.util.TokenCountingUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClientRequestLogger {

    private final RequestLogService requestLogService;
    private final TokenCountingUtils tokenCountingUtils;
    private final BillingUtils billingUtils;
    private final UserService userService;

    // 统一的请求状态跟踪
    private static final Map<String, LogState> requestStateMap = new ConcurrentHashMap<>();


    /**
     * 开始记录请求日志（通用）
     */
    public void startRequest(ChatCompletionRequest request, boolean isStream) {
        RequestContext ctx = RequestContextHolder.getContext();
        String requestId = ctx.getRequestId();
        LogState state = new LogState();
        state.setStartTime(LocalDateTime.now());
        state.setStream(isStream);
        // 初始使用计数器，防止流式没有返回usage或者流式中断
        Usage usage = new Usage();
        usage.setPromptTokens(tokenCountingUtils.countRequestTokens(request));
        usage.setCompletionTokens(0); // 初始为0，后续根据响应更新
        usage.setTotalTokens(usage.getPromptTokens());
        state.setUsage(usage);
        requestStateMap.put(requestId, state);
    }

    /**
     * 开始记录Anthropic请求日志
     */
    public void startRequest(AnthropicChatRequest request, boolean isStream) {
        RequestContext ctx = RequestContextHolder.getContext();
        String requestId = ctx.getRequestId();
        LogState state = new LogState();
        state.setStartTime(LocalDateTime.now());
        state.setStream(isStream);
        // 初始使用计数器，防止流式没有返回usage或者流式中断
        Usage usage = new Usage();
        usage.setPromptTokens(tokenCountingUtils.countRequestTokens(request));
        usage.setCompletionTokens(0); // 初始为0，后续根据响应更新
        usage.setTotalTokens(usage.getPromptTokens());
        state.setUsage(usage);
        requestStateMap.put(requestId, state);
    }


    /**
     * OpenAI 非流式
     */
    public void completeLog(ChatCompletionResponse response, boolean isSuccess) {
        RequestContext ctx = RequestContextHolder.getContext();
        String requestId = ctx.getRequestId();
        LogState state = requestStateMap.get(requestId);
        if (response.getUsage() != null) {
            // 有usage信息，直接使用
            state.setUsage(response.getUsage());
            state.setTokenSource("usage");
        } else {
            // 没返回判断不了有没有缓存，当做没有
            Usage usage = new Usage();
            // 保留开始日志时计算的 promptTokens
            usage.setPromptTokens(state.getUsage().getPromptTokens());
            usage.setCompletionTokens(tokenCountingUtils.countResponseTokens(response));
            usage.setTotalTokens(usage.getPromptTokens() + usage.getCompletionTokens());
            state.setUsage(usage);
        }
        createLog(isSuccess);
    }

    /**
     * Anthropic 非流式
     */
    public void completeLog(AnthropicMessage response, boolean isSuccess) {
        RequestContext ctx = RequestContextHolder.getContext();
        String requestId = ctx.getRequestId();
        LogState state = requestStateMap.get(requestId);
        if (response.getUsage() != null) {
            // 有usage信息，直接使用
            Usage usage = new Usage();
            usage.setPromptTokens(response.getUsage().getInputTokens());
            usage.setCompletionTokens(response.getUsage().getOutputTokens());
            usage.setTotalTokens(usage.getPromptTokens() + usage.getCompletionTokens());
            // 处理缓存tokens
            if (response.getUsage().getCacheReadInputTokens() != null) {
                if (usage.getPromptTokensDetails() == null) {
                    usage.setPromptTokensDetails(new PromptTokensDetails());
                }
                usage.getPromptTokensDetails().setCachedTokens(response.getUsage().getCacheReadInputTokens());
            }
            state.setUsage(usage);
            state.setTokenSource("usage");
        } else {
            // 没返回usage，使用计数器
            Usage usage = new Usage();
            // 保留开始日志时计算的 promptTokens
            usage.setPromptTokens(state.getUsage().getPromptTokens());
            usage.setCompletionTokens(tokenCountingUtils.countResponseTokens(response, ctx.getVendorModel().getVendorModelKey()));
            usage.setTotalTokens(usage.getPromptTokens() + usage.getCompletionTokens());
            state.setUsage(usage);
        }
        createLog(isSuccess);
    }

    public void logStreamChunk(ChatCompletionChunk chunk) {
        RequestContext ctx = RequestContextHolder.getContext();
        LogState state = requestStateMap.get(ctx.getRequestId());
        if (state == null) {
            log.warn("No logging state found for requestId: {}", ctx.getRequestId());
            return;
        }
        if (state.getFirstTokenLatencyMs() == null && chunk.getChoices() != null && !chunk.getChoices().isEmpty()) {
            state.setFirstTokenLatencyMs(calculateDurationMs(state.getStartTime(), LocalDateTime.now()));
        }
        if (chunk.getUsage() != null) {
            // 有usage信息，直接使用
            state.setUsage(chunk.getUsage());
            state.setTokenSource("usage");
        }
        // tokenSource是counter，说明没返回usage继续累加
        if (chunk.getChoices() != null && !chunk.getChoices().isEmpty() && state.getTokenSource().equals("counter")) {
            // 计算已生成的令牌数，防止流式中断或者没有usage
            int completionTokens = tokenCountingUtils.countChunkTokens(chunk, ctx.getVendorModel().getVendorModelKey());
            state.getUsage().setCompletionTokens(state.getUsage().getCompletionTokens() + completionTokens);
            state.getUsage().setTotalTokens(state.getUsage().getPromptTokens() + state.getUsage().getCompletionTokens());
        }
    }

    /**
     * Anthropic 流式
     */
    public void logStreamChunk(AnthropicStreamEvent event) {
        RequestContext ctx = RequestContextHolder.getContext();
        LogState state = requestStateMap.get(ctx.getRequestId());
        if (state == null) {
            log.warn("No logging state found for requestId: {}", ctx.getRequestId());
            return;
        }
        
        // 记录首token延迟（对于message_start或content_block_start事件）
        if (state.getFirstTokenLatencyMs() == null && 
            (event instanceof AnthropicMessageStartEvent || 
             event instanceof AnthropicContentBlockStartEvent)) {
            state.setFirstTokenLatencyMs(calculateDurationMs(state.getStartTime(), LocalDateTime.now()));
        }
        
        // 检查是否有usage信息（message_delta事件可能包含usage）
        if (event instanceof AnthropicMessageDeltaEvent) {
            AnthropicMessageDeltaEvent messageDeltaEvent = (AnthropicMessageDeltaEvent) event;
            if (messageDeltaEvent.getUsage() != null) {
                Usage usage = new Usage();
                usage.setPromptTokens(messageDeltaEvent.getUsage().getInputTokens());
                usage.setCompletionTokens(messageDeltaEvent.getUsage().getOutputTokens());
                usage.setTotalTokens(usage.getPromptTokens() + usage.getCompletionTokens());
                // 处理缓存tokens
                if (messageDeltaEvent.getUsage().getCacheReadInputTokens() != null) {
                    if (usage.getPromptTokensDetails() == null) {
                        usage.setPromptTokensDetails(new PromptTokensDetails());
                    }
                    usage.getPromptTokensDetails().setCachedTokens(messageDeltaEvent.getUsage().getCacheReadInputTokens());
                }
                state.setUsage(usage);
                state.setTokenSource("usage");
            }
        }
        
        // tokenSource是counter，说明没返回usage继续累加
        if (state.getTokenSource().equals("counter")) {
            // 计算已生成的令牌数，防止流式中断或者没有usage
            int completionTokens = tokenCountingUtils.countChunkTokens(event, ctx.getVendorModel().getVendorModelKey());
            if (completionTokens > 0) {
                state.getUsage().setCompletionTokens(state.getUsage().getCompletionTokens() + completionTokens);
                state.getUsage().setTotalTokens(state.getUsage().getPromptTokens() + state.getUsage().getCompletionTokens());
            }
        }
    }

    /**
     * 通用
     */
    public void completeLog(boolean isSuccess) {
        createLog(isSuccess);
    }

    private int calculateDurationMs(LocalDateTime startTime, LocalDateTime endTime) {
        return (int) Duration.between(startTime, endTime).toMillis();
    }


    private void createLog(boolean isSuccess) {
        RequestContext ctx = RequestContextHolder.getContext();
        String requestId = ctx.getRequestId();
        LogState state = requestStateMap.get(requestId);
        if (state == null) {
            log.warn("No logging state found for requestId: {}", requestId);
            return;
        }
        
        try {
            RequestLog.RequestLogBuilder builder = buildBaseRequestLog();
            builder.isSuccess(isSuccess);
            Usage usage = state.getUsage();
            
            // 做一下deepseek适配
            if (usage != null && usage.getPromptCacheHitTokens() != null) {
                if (usage.getPromptTokensDetails() == null) {
                    usage.setPromptTokensDetails(new PromptTokensDetails());
                }
                usage.getPromptTokensDetails().setCachedTokens(usage.getPromptCacheHitTokens());
            }
            
            if (isSuccess && usage != null) {
                builder.promptTokens(usage.getPromptTokens())
                       .cachedTokens(usage.getPromptTokensDetails() != null ? usage.getPromptTokensDetails().getCachedTokens() : null)
                       .completionTokens(usage.getCompletionTokens())
                       .totalTokens(usage.getTotalTokens());
                BillingUtils.BillingResult billingResult = billingUtils.calculateCost(usage);
                builder.inputCost(billingResult.inputCost())
                       .outputCost(billingResult.outputCost())
                       .totalCost(billingResult.totalCost())
                       .currency(billingResult.currency());
            }
            
            if (isSuccess && state.getStartTime() != null) {
                int durationMs = calculateDurationMs(state.getStartTime(), LocalDateTime.now());
                builder.durationMs(durationMs);
                if (usage != null && usage.getCompletionTokens() != null && durationMs > 0) {
                    BigDecimal tokensPerSecond = BigDecimal.valueOf(usage.getCompletionTokens())
                            .divide(BigDecimal.valueOf(durationMs), 10, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(1000))
                            .setScale(2, RoundingMode.HALF_UP);
                    builder.tokensPerSecond(tokensPerSecond);
                }
            }
            
            builder.tokenSource(state.getTokenSource()).isStream(state.stream);
            RequestLog logEntry = builder.build();
            log.info("[ClientRequestLogger]RequestLog: {}", logEntry);
            requestLogService.save(logEntry);
            
            // 实时扣减用户credits
            if (isSuccess && logEntry.getUserId() != null && logEntry.getTotalCost() != null) {
                try {
                    userService.deductUserCredits(logEntry.getUserId(), logEntry.getTotalCost());
                } catch (InsufficientCreditsException e) {
                    log.warn("用户credits不足: userId={}, cost={}, error={}", 
                            logEntry.getUserId(), logEntry.getTotalCost(), e.getMessage());
                    // 可以在这里添加额外的处理逻辑，比如通知用户或记录特殊日志
                } catch (Exception e) {
                    log.error("扣减用户credits失败: userId={}, cost={}, error={}", 
                            logEntry.getUserId(), logEntry.getTotalCost(), e.getMessage(), e);
                }
            }
        } finally {
            requestStateMap.remove(requestId);
        }
    }
    


    private RequestLog.RequestLogBuilder buildBaseRequestLog() {
        RequestLog.RequestLogBuilder builder = RequestLog.builder();
        RequestContext ctx = RequestContextHolder.getContext();
        LogState state = requestStateMap.get(ctx.getRequestId());
        if (state == null) {
            log.warn("No logging state found for requestId: {}", ctx.getRequestId());
            return builder;
        }
        log.debug("EndpointType: {}", ctx.getEndpointType());
        builder.firstTokenLatencyMs(state.getFirstTokenLatencyMs())
                .userId(ctx.getUserId())
                .apiKeyId(ctx.getApiKeyId())
                .requestId(ctx.getRequestId())
                .providerId(ctx.getProviderId())
                .modelKey(ctx.getVendorModel().getModelKey())
                .vendorModelKey(ctx.getVendorModel().getVendorModelKey())
                .endpointType(ctx.getEndpointType());

        return builder;
    }


    // 统一的请求状态跟踪类
    @Data
    private static class LogState {
        private LocalDateTime startTime;
        private Integer firstTokenLatencyMs;
        private String tokenSource = "counter"; // 默认使用计数器
        private boolean stream;
        private Usage usage; // 存储Usage信息用于详细计费
    }
}
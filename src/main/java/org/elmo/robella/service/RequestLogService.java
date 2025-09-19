package org.elmo.robella.service;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elmo.robella.model.entity.RequestLog;
import org.elmo.robella.model.entity.VendorModel;
import org.elmo.robella.repository.RequestLogRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class RequestLogService {

    private final RequestLogRepository requestLogRepository;

    private static final Map<String, LocalDateTime> requestStartTimeMap = new ConcurrentHashMap<>();

    public void logRequestStart(String requestId) {
        requestStartTimeMap.put(requestId, LocalDateTime.now());
    }

    public Mono<RequestLog> createRequestLog(RequestLog requestLog) {
        return requestLogRepository.save(requestLog)
                .doOnSuccess(savedLog -> log.debug("Request log saved: {}", savedLog.getId()))
                .doOnError(error -> log.error("Failed to save request log: {}", error.getMessage()));
    }

    public Mono<RequestLog> createSuccessLog(RequestLog requestLog) {
        LocalDateTime startTime = requestStartTimeMap.remove(requestLog.getRequestId());
        if (startTime == null) {
            startTime = LocalDateTime.now();
        }

        // 计算总持续时间
        Integer durationMs = calculateDurationMs(startTime);

        // 对于流式请求，使用专门的token计算方法
        String calculationMethod = "vendor_api";
        String tokenSource = requestLog.getTokenSource() != null ? requestLog.getTokenSource() : "response";

        // 如果是流式响应且没有token信息，尝试估算
        if (Boolean.TRUE.equals(requestLog.getIsStream()) && requestLog.getTotalTokens() == null) {
            calculationMethod = "stream_estimated";
            tokenSource = "stream_estimated";
        }

        // 获取VendorModel用于成本计算
        VendorModel vendorModel = null;
        if (requestLog.getProviderId() != null && requestLog.getVendorModelName() != null) {
            // 这里需要从数据库获取VendorModel，但为了简化，我们使用现有的逻辑
            // 实际项目中可能需要通过VendorModelRepository获取
        }

        RequestLog.RequestLogBuilder builder = requestLog.toBuilder()
                .durationMs(durationMs)
                .tokenSource(tokenSource)
                .calculationMethod(calculationMethod)
                .inputCost(calculateInputCost(requestLog.getPromptTokens(), vendorModel))
                .outputCost(calculateOutputCost(requestLog.getCompletionTokens(), vendorModel))
                .totalCost(calculateTotalCost(requestLog.getPromptTokens(), requestLog.getCompletionTokens(), vendorModel))
                .currency(vendorModel != null ? vendorModel.getCurrency() : "USD")
                .tokensPerSecond(calculateTokensPerSecond(requestLog.getTotalTokens(), durationMs))
                .isSuccess(true);

        return createRequestLog(builder.build());
    }

    public Mono<RequestLog> createFailureLog(RequestLog requestLog) {
        LocalDateTime startTime = requestStartTimeMap.remove(requestLog.getRequestId());
        if (startTime == null) {
            startTime = LocalDateTime.now();
        }

        RequestLog.RequestLogBuilder builder = requestLog.toBuilder()
                .durationMs(calculateDurationMs(startTime))
                .isSuccess(false);

        return createRequestLog(builder.build());
    }

    private Integer calculateDurationMs(LocalDateTime startTime) {
        return java.time.Duration.between(startTime, LocalDateTime.now())
                .toMillisPart();
    }

    private BigDecimal calculateTokensPerSecond(Integer totalTokens, Integer durationMs) {
        if (totalTokens == null || durationMs == null || durationMs == 0) {
            return null;
        }

        double tokensPerSecond = (totalTokens.doubleValue() / durationMs) * 1000;
        return BigDecimal.valueOf(tokensPerSecond).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateInputCost(Integer promptTokens, VendorModel vendorModel) {
        if (promptTokens == null || vendorModel == null || vendorModel.getInputPerMillionTokens() == null) {
            return BigDecimal.ZERO;
        }

        return vendorModel.getInputPerMillionTokens()
                .multiply(BigDecimal.valueOf(promptTokens))
                .divide(BigDecimal.valueOf(1_000_000), 6, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateOutputCost(Integer completionTokens, VendorModel vendorModel) {
        if (completionTokens == null || vendorModel == null || vendorModel.getOutputPerMillionTokens() == null) {
            return BigDecimal.ZERO;
        }

        return vendorModel.getOutputPerMillionTokens()
                .multiply(BigDecimal.valueOf(completionTokens))
                .divide(BigDecimal.valueOf(1_000_000), 6, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateTotalCost(Integer promptTokens, Integer completionTokens, VendorModel vendorModel) {
        BigDecimal inputCost = calculateInputCost(promptTokens, vendorModel);
        BigDecimal outputCost = calculateOutputCost(completionTokens, vendorModel);
        return inputCost.add(outputCost);
    }

    public Flux<RequestLog> getUserLogs(Long userId) {
        return requestLogRepository.findByUserId(userId);
    }

    public Flux<RequestLog> getUserLogsBetweenDates(Long userId, LocalDateTime startTime, LocalDateTime endTime) {
        return requestLogRepository.findByUserIdAndCreatedAtBetween(userId, startTime, endTime);
    }

    public Flux<RequestLog> getApiKeyLogs(Long apiKeyId) {
        return requestLogRepository.findByApiKeyId(apiKeyId);
    }

    public Flux<RequestLog> getRecentLogs() {
        return requestLogRepository.findTop100ByOrderByCreatedAtDesc();
    }

    public Mono<Long> getUserRequestCount(Long userId, LocalDateTime startTime, LocalDateTime endTime) {
        return requestLogRepository.countByUserIdAndCreatedAtBetween(userId, startTime, endTime);
    }

    public Mono<Long> getUserTotalTokens(Long userId, LocalDateTime startTime, LocalDateTime endTime) {
        return requestLogRepository.sumTotalTokensByUserIdAndCreatedAtBetween(userId, startTime, endTime);
    }

    public Mono<BigDecimal> getUserTotalCost(Long userId, LocalDateTime startTime, LocalDateTime endTime) {
        return requestLogRepository.sumTotalCostByUserIdAndCreatedAtBetween(userId, startTime, endTime);
    }

  }
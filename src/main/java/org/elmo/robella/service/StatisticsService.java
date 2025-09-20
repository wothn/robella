package org.elmo.robella.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elmo.robella.model.entity.RequestLog;
import org.elmo.robella.model.response.*;
import org.elmo.robella.repository.RequestLogRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final RequestLogRepository requestLogRepository;

    public Mono<SystemOverviewResponse> getSystemOverview(OffsetDateTime startTime, OffsetDateTime endTime) {
        return requestLogRepository.findByCreatedAtBetween(startTime, endTime)
                .collectList()
                .map(this::calculateSystemOverview);
    }

    public Mono<UserOverviewResponse> getUserOverview(Long userId, OffsetDateTime startTime, OffsetDateTime endTime) {
        return requestLogRepository.findByUserIdAndCreatedAtBetween(userId, startTime, endTime)
                .collectList()
                .map(logs -> calculateUserOverview(userId, logs, startTime, endTime));
    }

    public Mono<TokenUsageResponse> getTokenUsage(Long userId, OffsetDateTime startTime, OffsetDateTime endTime) {
        Flux<RequestLog> logsFlux = userId != null
                ? requestLogRepository.findByUserIdAndCreatedAtBetween(userId, startTime, endTime)
                : requestLogRepository.findByCreatedAtBetween(startTime, endTime);

        return logsFlux.collectList()
                .map(this::calculateTokenUsage);
    }

    public Mono<CostUsageResponse> getCostUsage(Long userId, OffsetDateTime startTime, OffsetDateTime endTime) {
        Flux<RequestLog> logsFlux = userId != null
                ? requestLogRepository.findByUserIdAndCreatedAtBetween(userId, startTime, endTime)
                : requestLogRepository.findByCreatedAtBetween(startTime, endTime);

        return logsFlux.collectList()
                .map(logs -> calculateCostUsage(logs, startTime, endTime));
    }

    public Mono<RequestUsageResponse> getRequestUsage(Long userId, OffsetDateTime startTime, OffsetDateTime endTime) {
        Flux<RequestLog> logsFlux = userId != null
                ? requestLogRepository.findByUserIdAndCreatedAtBetween(userId, startTime, endTime)
                : requestLogRepository.findByCreatedAtBetween(startTime, endTime);

        return logsFlux.collectList()
                .map(logs -> calculateRequestUsage(logs, startTime, endTime));
    }

    public Mono<LatencyStatsResponse> getLatencyStats(Long userId, OffsetDateTime startTime, OffsetDateTime endTime) {
        Flux<RequestLog> logsFlux = userId != null
                ? requestLogRepository.findByUserIdAndCreatedAtBetween(userId, startTime, endTime)
                : requestLogRepository.findByCreatedAtBetween(startTime, endTime);

        return logsFlux.filter(log -> log.getDurationMs() != null)
                .map(RequestLog::getDurationMs)
                .collectList()
                .map(this::calculateLatencyStats);
    }

    public Mono<TokenSpeedResponse> getTokenSpeedStats(Long userId, OffsetDateTime startTime, OffsetDateTime endTime) {
        Flux<RequestLog> logsFlux = userId != null
                ? requestLogRepository.findByUserIdAndCreatedAtBetween(userId, startTime, endTime)
                : requestLogRepository.findByCreatedAtBetween(startTime, endTime);

        return logsFlux.filter(log -> log.getTokensPerSecond() != null)
                .map(RequestLog::getTokensPerSecond)
                .collectList()
                .map(this::calculateTokenSpeedStats);
    }

    public Mono<ModelPopularityResponse> getModelPopularity(Long userId, OffsetDateTime startTime, OffsetDateTime endTime, int limit) {
        Flux<RequestLog> logsFlux = userId != null
                ? requestLogRepository.findByUserIdAndCreatedAtBetween(userId, startTime, endTime)
                : requestLogRepository.findByCreatedAtBetween(startTime, endTime);

        return logsFlux.collectList()
                .map(logs -> calculateModelPopularity(logs, startTime, endTime, limit));
    }

    public Mono<ModelCostResponse> getModelCosts(Long userId, OffsetDateTime startTime, OffsetDateTime endTime) {
        Flux<RequestLog> logsFlux = userId != null
                ? requestLogRepository.findByUserIdAndCreatedAtBetween(userId, startTime, endTime)
                : requestLogRepository.findByCreatedAtBetween(startTime, endTime);

        return logsFlux.collectList()
                .map(logs -> calculateModelCosts(logs, startTime, endTime));
    }

    public Mono<TimeSeriesResponse> getUsageTimeSeries(Long userId, OffsetDateTime startTime, OffsetDateTime endTime, String interval) {
        Flux<RequestLog> logsFlux = userId != null
                ? requestLogRepository.findByUserIdAndCreatedAtBetween(userId, startTime, endTime)
                : requestLogRepository.findByCreatedAtBetween(startTime, endTime);

        return logsFlux.collectList()
                .map(logs -> calculateTimeSeries(logs, startTime, endTime, interval));
    }

    public Mono<TimeSeriesResponse> getCostTimeSeries(Long userId, OffsetDateTime startTime, OffsetDateTime endTime, String interval) {
        return getUsageTimeSeries(userId, startTime, endTime, interval);
    }

    public Mono<ErrorRateResponse> getErrorRate(Long userId, OffsetDateTime startTime, OffsetDateTime endTime) {
        Flux<RequestLog> logsFlux = userId != null
                ? requestLogRepository.findByUserIdAndCreatedAtBetween(userId, startTime, endTime)
                : requestLogRepository.findByCreatedAtBetween(startTime, endTime);

        return logsFlux.collectList()
                .map(this::calculateErrorRate);
    }

    public Mono<ErrorByModelResponse> getErrorsByModel(Long userId, OffsetDateTime startTime, OffsetDateTime endTime) {
        Flux<RequestLog> logsFlux = userId != null
                ? requestLogRepository.findByUserIdAndCreatedAtBetween(userId, startTime, endTime)
                : requestLogRepository.findByCreatedAtBetween(startTime, endTime);

        return logsFlux.collectList()
                .map(logs -> calculateErrorsByModel(logs, startTime, endTime));
    }

    private SystemOverviewResponse calculateSystemOverview(List<RequestLog> logs) {
        if (logs.isEmpty()) {
            return SystemOverviewResponse.builder()
                    .totalRequests(0L)
                    .successfulRequests(0L)
                    .failedRequests(0L)
                    .totalTokens(0L)
                    .totalCost(BigDecimal.ZERO)
                    .averageDurationMs(0.0)
                    .averageTokensPerSecond(0.0)
                    .errorRate(0.0)
                    .activeUsers(0)
                    .uniqueModels(0)
                    .periodStart(logs.isEmpty() ? null : logs.get(0).getCreatedAt())
                    .periodEnd(logs.isEmpty() ? null : logs.get(logs.size() - 1).getCreatedAt())
                    .build();
        }

        long totalRequests = logs.size();
        long successfulRequests = logs.stream().filter(log -> Boolean.TRUE.equals(log.getIsSuccess())).count();
        long failedRequests = totalRequests - successfulRequests;
        long totalTokens = logs.stream().mapToLong(log -> log.getTotalTokens() != null ? log.getTotalTokens() : 0).sum();
        BigDecimal totalCost = logs.stream()
                .map(log -> log.getTotalCost() != null ? log.getTotalCost() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        double avgDuration = logs.stream()
                .filter(log -> log.getDurationMs() != null)
                .mapToInt(RequestLog::getDurationMs)
                .average()
                .orElse(0.0);

        double avgTokensPerSecond = logs.stream()
                .filter(log -> log.getTokensPerSecond() != null)
                .mapToDouble(log -> log.getTokensPerSecond().doubleValue())
                .average()
                .orElse(0.0);

        Set<Long> uniqueUsers = logs.stream()
                .map(RequestLog::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<String> uniqueModels = logs.stream()
                .map(RequestLog::getModelKey)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        return SystemOverviewResponse.builder()
                .totalRequests(totalRequests)
                .successfulRequests(successfulRequests)
                .failedRequests(failedRequests)
                .totalTokens(totalTokens)
                .totalCost(totalCost)
                .averageDurationMs(avgDuration)
                .averageTokensPerSecond(avgTokensPerSecond)
                .errorRate(totalRequests > 0 ? (failedRequests * 100.0 / totalRequests) : 0.0)
                .activeUsers(uniqueUsers.size())
                .uniqueModels(uniqueModels.size())
                .periodStart(logs.get(0).getCreatedAt())
                .periodEnd(logs.get(logs.size() - 1).getCreatedAt())
                .build();
    }

    private UserOverviewResponse calculateUserOverview(Long userId, List<RequestLog> logs, OffsetDateTime startTime, OffsetDateTime endTime) {
        if (logs.isEmpty()) {
            return UserOverviewResponse.builder()
                    .userId(userId)
                    .totalRequests(0L)
                    .successfulRequests(0L)
                    .failedRequests(0L)
                    .totalTokens(0L)
                    .totalCost(BigDecimal.ZERO)
                    .averageDurationMs(0.0)
                    .averageTokensPerSecond(0.0)
                    .errorRate(0.0)
                    .uniqueModelsUsed(0)
                    .periodStart(startTime)
                    .periodEnd(endTime)
                    .build();
        }

        long totalRequests = logs.size();
        long successfulRequests = logs.stream().filter(log -> Boolean.TRUE.equals(log.getIsSuccess())).count();
        long failedRequests = totalRequests - successfulRequests;
        long totalTokens = logs.stream().mapToLong(log -> log.getTotalTokens() != null ? log.getTotalTokens() : 0).sum();
        BigDecimal totalCost = logs.stream()
                .map(log -> log.getTotalCost() != null ? log.getTotalCost() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        double avgDuration = logs.stream()
                .filter(log -> log.getDurationMs() != null)
                .mapToInt(RequestLog::getDurationMs)
                .average()
                .orElse(0.0);

        double avgTokensPerSecond = logs.stream()
                .filter(log -> log.getTokensPerSecond() != null)
                .mapToDouble(log -> log.getTokensPerSecond().doubleValue())
                .average()
                .orElse(0.0);

        Set<String> uniqueModels = logs.stream()
                .map(RequestLog::getModelKey)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        return UserOverviewResponse.builder()
                .userId(userId)
                .totalRequests(totalRequests)
                .successfulRequests(successfulRequests)
                .failedRequests(failedRequests)
                .totalTokens(totalTokens)
                .totalCost(totalCost)
                .averageDurationMs(avgDuration)
                .averageTokensPerSecond(avgTokensPerSecond)
                .errorRate(totalRequests > 0 ? (failedRequests * 100.0 / totalRequests) : 0.0)
                .uniqueModelsUsed(uniqueModels.size())
                .periodStart(startTime)
                .periodEnd(endTime)
                .build();
    }

    private TokenUsageResponse calculateTokenUsage(List<RequestLog> logs) {
        if (logs.isEmpty()) {
            return TokenUsageResponse.builder()
                    .totalPromptTokens(0L)
                    .totalCompletionTokens(0L)
                    .totalTokens(0L)
                    .averagePromptTokensPerRequest(0.0)
                    .averageCompletionTokensPerRequest(0.0)
                    .averageTokensPerRequest(0.0)
                    .periodStart(null)
                    .periodEnd(null)
                    .build();
        }

        long totalPromptTokens = logs.stream().mapToLong(log -> log.getPromptTokens() != null ? log.getPromptTokens() : 0).sum();
        long totalCompletionTokens = logs.stream().mapToLong(log -> log.getCompletionTokens() != null ? log.getCompletionTokens() : 0).sum();
        long totalTokens = logs.stream().mapToLong(log -> log.getTotalTokens() != null ? log.getTotalTokens() : 0).sum();

        double avgPromptTokens = logs.stream()
                .filter(log -> log.getPromptTokens() != null)
                .mapToInt(RequestLog::getPromptTokens)
                .average()
                .orElse(0.0);

        double avgCompletionTokens = logs.stream()
                .filter(log -> log.getCompletionTokens() != null)
                .mapToInt(RequestLog::getCompletionTokens)
                .average()
                .orElse(0.0);

        double avgTokens = logs.stream()
                .filter(log -> log.getTotalTokens() != null)
                .mapToInt(RequestLog::getTotalTokens)
                .average()
                .orElse(0.0);

        return TokenUsageResponse.builder()
                .totalPromptTokens(totalPromptTokens)
                .totalCompletionTokens(totalCompletionTokens)
                .totalTokens(totalTokens)
                .averagePromptTokensPerRequest(avgPromptTokens)
                .averageCompletionTokensPerRequest(avgCompletionTokens)
                .averageTokensPerRequest(avgTokens)
                .periodStart(logs.get(0).getCreatedAt())
                .periodEnd(logs.get(logs.size() - 1).getCreatedAt())
                .build();
    }

    private CostUsageResponse calculateCostUsage(List<RequestLog> logs, OffsetDateTime startTime, OffsetDateTime endTime) {
        if (logs.isEmpty()) {
            return CostUsageResponse.builder()
                    .totalInputCost(BigDecimal.ZERO)
                    .totalOutputCost(BigDecimal.ZERO)
                    .totalCost(BigDecimal.ZERO)
                    .averageInputCostPerRequest(0.0)
                    .averageOutputCostPerRequest(0.0)
                    .averageCostPerRequest(0.0)
                    .averageCostPerToken(BigDecimal.ZERO)
                    .currency("USD")
                    .periodStart(startTime)
                    .periodEnd(endTime)
                    .build();
        }

        BigDecimal totalInputCost = logs.stream()
                .map(log -> log.getInputCost() != null ? log.getInputCost() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalOutputCost = logs.stream()
                .map(log -> log.getOutputCost() != null ? log.getOutputCost() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCost = logs.stream()
                .map(log -> log.getTotalCost() != null ? log.getTotalCost() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        double avgInputCost = logs.stream()
                .filter(log -> log.getInputCost() != null)
                .mapToDouble(log -> log.getInputCost().doubleValue())
                .average()
                .orElse(0.0);

        double avgOutputCost = logs.stream()
                .filter(log -> log.getOutputCost() != null)
                .mapToDouble(log -> log.getOutputCost().doubleValue())
                .average()
                .orElse(0.0);

        double avgCost = logs.stream()
                .filter(log -> log.getTotalCost() != null)
                .mapToDouble(log -> log.getTotalCost().doubleValue())
                .average()
                .orElse(0.0);

        long totalTokens = logs.stream().mapToLong(log -> log.getTotalTokens() != null ? log.getTotalTokens() : 0).sum();
        BigDecimal avgCostPerToken = totalTokens > 0 ? totalCost.divide(BigDecimal.valueOf(totalTokens), 6, RoundingMode.HALF_UP) : BigDecimal.ZERO;

        String currency = logs.stream()
                .filter(log -> log.getCurrency() != null)
                .map(RequestLog::getCurrency)
                .findFirst()
                .orElse("USD");

        return CostUsageResponse.builder()
                .totalInputCost(totalInputCost)
                .totalOutputCost(totalOutputCost)
                .totalCost(totalCost)
                .averageInputCostPerRequest(avgInputCost)
                .averageOutputCostPerRequest(avgOutputCost)
                .averageCostPerRequest(avgCost)
                .averageCostPerToken(avgCostPerToken)
                .currency(currency)
                .periodStart(logs.get(0).getCreatedAt())
                .periodEnd(logs.get(logs.size() - 1).getCreatedAt())
                .build();
    }

    private RequestUsageResponse calculateRequestUsage(List<RequestLog> logs, OffsetDateTime startTime, OffsetDateTime endTime) {
        if (logs.isEmpty()) {
            return RequestUsageResponse.builder()
                    .totalRequests(0L)
                    .successfulRequests(0L)
                    .failedRequests(0L)
                    .streamRequests(0L)
                    .nonStreamRequests(0L)
                    .successRate(0.0)
                    .errorRate(0.0)
                    .streamRate(0.0)
                    .periodStart(startTime)
                    .periodEnd(endTime)
                    .build();
        }

        long totalRequests = logs.size();
        long successfulRequests = logs.stream().filter(log -> Boolean.TRUE.equals(log.getIsSuccess())).count();
        long failedRequests = totalRequests - successfulRequests;
        long streamRequests = logs.stream().filter(log -> Boolean.TRUE.equals(log.getIsStream())).count();
        long nonStreamRequests = totalRequests - streamRequests;

        return RequestUsageResponse.builder()
                .totalRequests(totalRequests)
                .successfulRequests(successfulRequests)
                .failedRequests(failedRequests)
                .streamRequests(streamRequests)
                .nonStreamRequests(nonStreamRequests)
                .successRate(totalRequests > 0 ? (successfulRequests * 1.0 / totalRequests) : 0.0)
                .errorRate(totalRequests > 0 ? (failedRequests * 100.0 / totalRequests) : 0.0)
                .streamRate(totalRequests > 0 ? (streamRequests * 100.0 / totalRequests) : 0.0)
                .periodStart(logs.get(0).getCreatedAt())
                .periodEnd(logs.get(logs.size() - 1).getCreatedAt())
                .build();
    }

    private LatencyStatsResponse calculateLatencyStats(List<Integer> durations) {
        if (durations.isEmpty()) {
            return LatencyStatsResponse.builder()
                    .averageDurationMs(0.0)
                    .minDurationMs(0.0)
                    .maxDurationMs(0.0)
                    .medianDurationMs(0.0)
                    .p95DurationMs(0.0)
                    .p99DurationMs(0.0)
                    .averageFirstTokenLatencyMs(0.0)
                    .build();
        }

        Collections.sort(durations);
        double avg = durations.stream().mapToInt(Integer::intValue).average().orElse(0.0);
        double min = durations.get(0);
        double max = durations.get(durations.size() - 1);
        double median = durations.get(durations.size() / 2);
        double p95 = durations.get((int) (durations.size() * 0.95));
        double p99 = durations.get((int) (durations.size() * 0.99));

        return LatencyStatsResponse.builder()
                .averageDurationMs(avg)
                .minDurationMs(min)
                .maxDurationMs(max)
                .medianDurationMs(median)
                .p95DurationMs(p95)
                .p99DurationMs(p99)
                .averageFirstTokenLatencyMs(0.0) // TODO: Implement first token latency calculation
                .build();
    }

    private TokenSpeedResponse calculateTokenSpeedStats(List<BigDecimal> tokenSpeeds) {
        if (tokenSpeeds.isEmpty()) {
            return TokenSpeedResponse.builder()
                    .averageTokensPerSecond(BigDecimal.ZERO)
                    .maxTokensPerSecond(BigDecimal.ZERO)
                    .minTokensPerSecond(BigDecimal.ZERO)
                    .medianTokensPerSecond(BigDecimal.ZERO)
                    .build();
        }

        Collections.sort(tokenSpeeds);
        BigDecimal avg = tokenSpeeds.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(tokenSpeeds.size()), 6, RoundingMode.HALF_UP);
        BigDecimal min = tokenSpeeds.get(0);
        BigDecimal max = tokenSpeeds.get(tokenSpeeds.size() - 1);
        BigDecimal median = tokenSpeeds.get(tokenSpeeds.size() / 2);

        return TokenSpeedResponse.builder()
                .averageTokensPerSecond(avg)
                .maxTokensPerSecond(max)
                .minTokensPerSecond(min)
                .medianTokensPerSecond(median)
                .build();
    }

    private ModelPopularityResponse calculateModelPopularity(List<RequestLog> logs, OffsetDateTime startTime, OffsetDateTime endTime, int limit) {
        Map<String, ModelPopularityResponse.ModelStats> modelStatsMap = new HashMap<>();

        for (RequestLog log : logs) {
            if (log.getModelKey() == null) continue;

            ModelPopularityResponse.ModelStats stats = modelStatsMap.computeIfAbsent(log.getModelKey(), k ->
                ModelPopularityResponse.ModelStats.builder()
                    .modelKey(k)
                    .requestCount(0L)
                    .totalTokens(0L)
                    .totalCost(BigDecimal.ZERO)
                    .successRate(0.0)
                    .averageDurationMs(0.0)
                    .build());

            stats.setRequestCount(stats.getRequestCount() + 1);
            stats.setTotalTokens(stats.getTotalTokens() + (log.getTotalTokens() != null ? log.getTotalTokens() : 0));
            stats.setTotalCost(stats.getTotalCost().add(log.getTotalCost() != null ? log.getTotalCost() : BigDecimal.ZERO));
        }

        // Calculate success rates and average durations
        for (ModelPopularityResponse.ModelStats stats : modelStatsMap.values()) {
            List<RequestLog> modelLogs = logs.stream()
                    .filter(log -> log.getModelKey() != null && log.getModelKey().equals(stats.getModelKey()))
                    .collect(Collectors.toList());

            long successfulRequests = modelLogs.stream().filter(log -> Boolean.TRUE.equals(log.getIsSuccess())).count();
            stats.setSuccessRate(modelLogs.size() > 0 ? (successfulRequests * 1.0 / modelLogs.size()) : 0.0);

            double avgDuration = modelLogs.stream()
                    .filter(log -> log.getDurationMs() != null)
                    .mapToInt(RequestLog::getDurationMs)
                    .average()
                    .orElse(0.0);
            stats.setAverageDurationMs(avgDuration);
        }

        List<ModelPopularityResponse.ModelStats> sortedModels = modelStatsMap.values().stream()
                .sorted((a, b) -> Long.compare(b.getRequestCount(), a.getRequestCount()))
                .limit(limit)
                .collect(Collectors.toList());

        return ModelPopularityResponse.builder()
                .models(sortedModels)
                .periodStart(startTime)
                .periodEnd(endTime)
                .build();
    }

    private ModelCostResponse calculateModelCosts(List<RequestLog> logs, OffsetDateTime startTime, OffsetDateTime endTime) {
        Map<String, ModelCostResponse.ModelCostStats> modelCostMap = new HashMap<>();

        for (RequestLog log : logs) {
            if (log.getModelKey() == null) continue;

            ModelCostResponse.ModelCostStats stats = modelCostMap.computeIfAbsent(log.getModelKey(), k ->
                ModelCostResponse.ModelCostStats.builder()
                    .modelKey(k)
                    .totalCost(BigDecimal.ZERO)
                    .totalTokens(0L)
                    .averageCostPerToken(BigDecimal.ZERO)
                    .averageCostPerRequest(BigDecimal.ZERO)
                    .requestCount(0L)
                    .build());

            stats.setRequestCount(stats.getRequestCount() + 1);
            stats.setTotalTokens(stats.getTotalTokens() + (log.getTotalTokens() != null ? log.getTotalTokens() : 0));
            stats.setTotalCost(stats.getTotalCost().add(log.getTotalCost() != null ? log.getTotalCost() : BigDecimal.ZERO));
        }

        // Calculate averages
        for (ModelCostResponse.ModelCostStats stats : modelCostMap.values()) {
            if (stats.getTotalTokens() > 0) {
                stats.setAverageCostPerToken(stats.getTotalCost().divide(BigDecimal.valueOf(stats.getTotalTokens()), 6, RoundingMode.HALF_UP));
            }
            if (stats.getRequestCount() > 0) {
                stats.setAverageCostPerRequest(stats.getTotalCost().divide(BigDecimal.valueOf(stats.getRequestCount()), 6, RoundingMode.HALF_UP));
            }
        }

        return ModelCostResponse.builder()
                .models(new ArrayList<>(modelCostMap.values()))
                .periodStart(startTime)
                .periodEnd(endTime)
                .build();
    }

    private TimeSeriesResponse calculateTimeSeries(List<RequestLog> logs, OffsetDateTime startTime, OffsetDateTime endTime, String interval) {
        Map<String, TimeSeriesResponse.TimeSeriesDataPoint> dataPointMap = new HashMap<>();
        DateTimeFormatter formatter = getIntervalFormatter(interval);

        for (RequestLog log : logs) {
            String timeKey = log.getCreatedAt().format(formatter);

            TimeSeriesResponse.TimeSeriesDataPoint dataPoint = dataPointMap.computeIfAbsent(timeKey, k ->
                TimeSeriesResponse.TimeSeriesDataPoint.builder()
                    .timestamp(parseTimeKey(k, interval))
                    .requestCount(0L)
                    .totalTokens(0L)
                    .totalCost(BigDecimal.ZERO)
                    .successfulRequests(0L)
                    .failedRequests(0L)
                    .build());

            dataPoint.setRequestCount(dataPoint.getRequestCount() + 1);
            dataPoint.setTotalTokens(dataPoint.getTotalTokens() + (log.getTotalTokens() != null ? log.getTotalTokens() : 0));
            dataPoint.setTotalCost(dataPoint.getTotalCost().add(log.getTotalCost() != null ? log.getTotalCost() : BigDecimal.ZERO));

            if (Boolean.TRUE.equals(log.getIsSuccess())) {
                dataPoint.setSuccessfulRequests(dataPoint.getSuccessfulRequests() + 1);
            } else {
                dataPoint.setFailedRequests(dataPoint.getFailedRequests() + 1);
            }
        }

        List<TimeSeriesResponse.TimeSeriesDataPoint> sortedDataPoints = dataPointMap.values().stream()
                .sorted(Comparator.comparing(TimeSeriesResponse.TimeSeriesDataPoint::getTimestamp))
                .collect(Collectors.toList());

        return TimeSeriesResponse.builder()
                .interval(interval)
                .dataPoints(sortedDataPoints)
                .periodStart(startTime)
                .periodEnd(endTime)
                .build();
    }

    private ErrorRateResponse calculateErrorRate(List<RequestLog> logs) {
        if (logs.isEmpty()) {
            return ErrorRateResponse.builder()
                    .overallErrorRate(0.0)
                    .totalRequests(0L)
                    .failedRequests(0L)
                    .periodStart(null)
                    .periodEnd(null)
                    .build();
        }

        long totalRequests = logs.size();
        long failedRequests = logs.stream().filter(log -> Boolean.FALSE.equals(log.getIsSuccess())).count();

        return ErrorRateResponse.builder()
                .overallErrorRate(totalRequests > 0 ? (failedRequests * 100.0 / totalRequests) : 0.0)
                .totalRequests(totalRequests)
                .failedRequests(failedRequests)
                .periodStart(logs.get(0).getCreatedAt())
                .periodEnd(logs.get(logs.size() - 1).getCreatedAt())
                .build();
    }

    private ErrorByModelResponse calculateErrorsByModel(List<RequestLog> logs, OffsetDateTime startTime, OffsetDateTime endTime) {
        Map<String, ErrorByModelResponse.ModelErrorStats> modelErrorMap = new HashMap<>();

        for (RequestLog log : logs) {
            if (log.getModelKey() == null) continue;

            ErrorByModelResponse.ModelErrorStats stats = modelErrorMap.computeIfAbsent(log.getModelKey(), k ->
                ErrorByModelResponse.ModelErrorStats.builder()
                    .modelKey(k)
                    .totalRequests(0L)
                    .failedRequests(0L)
                    .errorRate(0.0)
                    .build());

            stats.setTotalRequests(stats.getTotalRequests() + 1);
            if (Boolean.FALSE.equals(log.getIsSuccess())) {
                stats.setFailedRequests(stats.getFailedRequests() + 1);
            }
        }

        // Calculate error rates
        for (ErrorByModelResponse.ModelErrorStats stats : modelErrorMap.values()) {
            if (stats.getTotalRequests() > 0) {
                stats.setErrorRate(stats.getFailedRequests() * 100.0 / stats.getTotalRequests());
            }
        }

        return ErrorByModelResponse.builder()
                .models(new ArrayList<>(modelErrorMap.values()))
                .periodStart(startTime)
                .periodEnd(endTime)
                .build();
    }

    private DateTimeFormatter getIntervalFormatter(String interval) {
        switch (interval.toLowerCase()) {
            case "minute":
                return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            case "hour":
                return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:00");
            case "day":
                return DateTimeFormatter.ofPattern("yyyy-MM-dd");
            case "week":
                return DateTimeFormatter.ofPattern("yyyy-ww");
            case "month":
                return DateTimeFormatter.ofPattern("yyyy-MM");
            default:
                return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:00");
        }
    }

    private OffsetDateTime parseTimeKey(String timeKey, String interval) {
        try {
            switch (interval.toLowerCase()) {
                case "minute":
                    return OffsetDateTime.parse(timeKey + ":00", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssXXX"));
                case "hour":
                    return OffsetDateTime.parse(timeKey + ":00:00", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssXXX"));
                case "day":
                    return OffsetDateTime.parse(timeKey + " 00:00:00", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssXXX"));
                case "week":
                    // Week parsing is more complex, simplified for now
                    return OffsetDateTime.parse(timeKey + "-1 00:00:00", DateTimeFormatter.ofPattern("yyyy-ww-ww HH:mm:ssXXX"));
                case "month":
                    return OffsetDateTime.parse(timeKey + "-01 00:00:00", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssXXX"));
                default:
                    return OffsetDateTime.parse(timeKey + ":00:00", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssXXX"));
            }
        } catch (Exception e) {
            return OffsetDateTime.now();
        }
    }
}
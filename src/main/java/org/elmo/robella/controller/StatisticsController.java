package org.elmo.robella.controller;

import org.elmo.robella.annotation.RequiredRole;
import org.elmo.robella.model.common.Role;
import org.elmo.robella.model.response.*;
import org.elmo.robella.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
@Slf4j
@Validated
public class StatisticsController {

    private final StatisticsService statisticsService;

    @GetMapping("/overview")
    @RequiredRole(Role.ADMIN)
    public SystemOverviewResponse getSystemOverview(
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startTime,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endTime) {
        return statisticsService.getSystemOverview(startTime, endTime);
    }

    @GetMapping("/overview/user/{userId}")
    public UserOverviewResponse getUserOverview(
            @PathVariable @NotNull Long userId,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startTime,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endTime) {
        return statisticsService.getUserOverview(userId, startTime, endTime);
    }

    @GetMapping("/usage/tokens")
    public TokenUsageResponse getTokenUsage(
            @RequestParam(required = false) Long userId,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startTime,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endTime) {
        return statisticsService.getTokenUsage(userId, startTime, endTime);
    }

    @GetMapping("/usage/costs")
    public CostUsageResponse getCostUsage(
            @RequestParam(required = false) Long userId,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startTime,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endTime) {
        return statisticsService.getCostUsage(userId, startTime, endTime);
    }

    @GetMapping("/usage/requests")
    public RequestUsageResponse getRequestUsage(
            @RequestParam(required = false) Long userId,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startTime,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endTime) {
        return statisticsService.getRequestUsage(userId, startTime, endTime);
    }

    @GetMapping("/performance/latency")
    public LatencyStatsResponse getLatencyStats(
            @RequestParam(required = false) Long userId,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startTime,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endTime) {
        return statisticsService.getLatencyStats(userId, startTime, endTime);
    }

    @GetMapping("/performance/tokens-per-second")
    public TokenSpeedResponse getTokenSpeedStats(
            @RequestParam(required = false) Long userId,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startTime,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endTime) {
        return statisticsService.getTokenSpeedStats(userId, startTime, endTime);
    }

    @GetMapping("/models/popularity")
    public ModelPopularityResponse getModelPopularity(
            @RequestParam(required = false) Long userId,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startTime,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endTime,
            @RequestParam(defaultValue = "10") int limit) {
        return statisticsService.getModelPopularity(userId, startTime, endTime, limit);
    }

    @GetMapping("/models/costs")
    public ModelCostResponse getModelCosts(
            @RequestParam(required = false) Long userId,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startTime,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endTime) {
        return statisticsService.getModelCosts(userId, startTime, endTime);
    }

    @GetMapping("/timeseries/usage")
    public TimeSeriesResponse getUsageTimeSeries(
            @RequestParam(required = false) Long userId,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startTime,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endTime,
            @RequestParam(defaultValue = "hour") String interval) {
        return statisticsService.getUsageTimeSeries(userId, startTime, endTime, interval);
    }

    @GetMapping("/timeseries/costs")
    public TimeSeriesResponse getCostTimeSeries(
            @RequestParam(required = false) Long userId,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startTime,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endTime,
            @RequestParam(defaultValue = "hour") String interval) {
        return statisticsService.getCostTimeSeries(userId, startTime, endTime, interval);
    }

    @GetMapping("/errors/rate")
    public ErrorRateResponse getErrorRate(
            @RequestParam(required = false) Long userId,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startTime,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endTime) {
        return statisticsService.getErrorRate(userId, startTime, endTime);
    }

    @GetMapping("/errors/by-model")
    public ErrorByModelResponse getErrorsByModel(
            @RequestParam(required = false) Long userId,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startTime,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endTime) {
        return statisticsService.getErrorsByModel(userId, startTime, endTime);
    }
}
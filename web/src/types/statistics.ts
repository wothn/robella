export interface SystemOverviewResponse {
  totalRequests: number
  successfulRequests: number
  failedRequests: number
  totalTokens: number
  totalCost: number
  averageDurationMs: number
  averageTokensPerSecond: number
  errorRate: number
  activeUsers: number
  uniqueModels: number
  periodStart: string
  periodEnd: string
}

export interface UserOverviewResponse {
  userId: number
  totalRequests: number
  totalTokens: number
  totalCost: number
  averageLatency: number
  period: string
}

export interface TokenUsageResponse {
  totalTokens: number
  totalPromptTokens: number
  totalCompletionTokens: number
  averagePromptTokensPerRequest: number
  averageCompletionTokensPerRequest: number
  averageTokensPerRequest: number
  periodStart: string
  periodEnd: string
}

export interface CostUsageResponse {
  totalInputCost: number
  totalOutputCost: number
  totalCost: number
  averageInputCostPerRequest: number
  averageOutputCostPerRequest: number
  averageCostPerRequest: number
  averageCostPerToken: number
  currency: string
  periodStart: string
  periodEnd: string
}

export interface RequestUsageResponse {
  totalRequests: number
  successfulRequests: number
  failedRequests: number
  streamRequests: number
  nonStreamRequests: number
  successRate: number
  errorRate: number
  streamRate: number
  periodStart: string
  periodEnd: string
}

export interface LatencyStatsResponse {
  averageDurationMs: number
  minDurationMs: number
  maxDurationMs: number
  medianDurationMs: number
  p95DurationMs: number
  p99DurationMs: number
  averageFirstTokenLatencyMs: number
  periodStart: string
  periodEnd: string
}

export interface TokenSpeedResponse {
  averageTokensPerSecond: number
  peakTokensPerSecond: number
  period: string
}

export interface ModelPopularityItem {
  modelKey: string
  requestCount: number
  totalTokens: number
  totalCost: number
  successRate: number
  averageDurationMs: number
}

export interface ModelPopularityResponse {
  models: ModelPopularityItem[]
  periodStart: string
  periodEnd: string
}

export interface ModelCostItem {
  modelName: string
  totalCost: number
  requestCount: number
  averageCostPerRequest: number
}

export interface ModelCostResponse {
  models: ModelCostItem[]
  period: string
}

export interface TimeSeriesDataPoint {
  timestamp: string
  value: number
}

export interface TimeSeriesResponse {
  data: TimeSeriesDataPoint[]
  period: string
  interval: string
}

export interface ErrorRateResponse {
  overallErrorRate: number
  totalRequests: number
  failedRequests: number
  periodStart: string
  periodEnd: string
}

export interface ModelErrorItem {
  modelName: string
  errorCount: number
  errorRate: number
}

export interface ErrorByModelResponse {
  models: ModelErrorItem[]
  period: string
}

export interface StatisticsRequestParams {
  startTime: string
  endTime: string
  userId?: number
  interval?: string
  limit?: number
}
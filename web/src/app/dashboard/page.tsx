import { useState, useEffect, useCallback } from "react"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { usePageLoading, withPageLoading } from "@/stores/loading-store"
import { PageLoading } from "@/components/common/loading"
import {
  TrendingUp,
  Zap,
  DollarSign,
  Activity,
  BarChart3,
  Timer,
  Target,
  AlertTriangle,
  RefreshCw
} from "lucide-react"
import { api } from "@/lib/api"
import type {
  SystemOverviewResponse,
  TokenUsageResponse,
  CostUsageResponse,
  RequestUsageResponse,
  LatencyStatsResponse,
  ModelPopularityResponse,
  ErrorRateResponse
} from "@/types/statistics"

const Dashboard = () => {
  const [timeRange, setTimeRange] = useState("24h")
  const { loading } = usePageLoading('dashboard')
  const [overview, setOverview] = useState<SystemOverviewResponse | null>(null)
  const [tokenUsage, setTokenUsage] = useState<TokenUsageResponse | null>(null)
  const [costUsage, setCostUsage] = useState<CostUsageResponse | null>(null)
  const [requestUsage, setRequestUsage] = useState<RequestUsageResponse | null>(null)
  const [latency, setLatency] = useState<LatencyStatsResponse | null>(null)
  const [popularModels, setPopularModels] = useState<ModelPopularityResponse | null>(null)
  const [errorRate, setErrorRate] = useState<ErrorRateResponse | null>(null)

  const getTimeRange = (range: string) => {
    const now = new Date()
    const formatDateTime = (date: Date) => {
      return date.toISOString()
    }

    const endTime = formatDateTime(now)
    let startTime: string

    switch (range) {
      case "1h":
        startTime = formatDateTime(new Date(now.getTime() - 60 * 60 * 1000))
        break
      case "24h":
        startTime = formatDateTime(new Date(now.getTime() - 24 * 60 * 60 * 1000))
        break
      case "7d":
        startTime = formatDateTime(new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000))
        break
      case "30d":
        startTime = formatDateTime(new Date(now.getTime() - 30 * 24 * 60 * 60 * 1000))
        break
      default:
        startTime = formatDateTime(new Date(now.getTime() - 24 * 60 * 60 * 1000))
    }

    return { startTime, endTime }
  }

  const loadStatistics = useCallback(async () => {
    return withPageLoading('dashboard', async () => {
      try {
        const { startTime, endTime } = getTimeRange(timeRange)

        const [
          overviewData,
          tokenData,
          costData,
          requestData,
          latencyData,
          modelsData,
          errorData
        ] = await Promise.all([
          api.getSystemOverview(startTime, endTime),
          api.getTokenUsage(startTime, endTime),
          api.getCostUsage(startTime, endTime),
          api.getRequestUsage(startTime, endTime),
          api.getLatencyStats(startTime, endTime),
          api.getModelPopularity(startTime, endTime, 5),
          api.getErrorRate(startTime, endTime)
        ])

        setOverview(overviewData)
        setTokenUsage(tokenData)
        setCostUsage(costData)
        setRequestUsage(requestData)
        setLatency(latencyData)
        setPopularModels(modelsData)
        setErrorRate(errorData)
      } catch (error) {
        console.error("Failed to load statistics:", error)
      }
    })
  }, [timeRange])

  useEffect(() => {
    loadStatistics()
  }, [timeRange, loadStatistics])

  const formatNumber = (num: number | undefined) => {
    if (num === undefined || num === null) return "0"
    if (num >= 1000000) {
      return `${(num / 1000000).toFixed(1)}M`
    }
    if (num >= 1000) {
      return `${(num / 1000).toFixed(1)}K`
    }
    return num.toString()
  }

  const formatCurrency = (amount: number) => {
    return `$${amount.toFixed(2)}`
  }

  const formatLatency = (ms: number | undefined) => {
    if (ms === undefined || ms === null) return "0ms"
    if (ms >= 1000) {
      return `${(ms / 1000).toFixed(1)}s`
    }
    return `${ms.toFixed(0)}ms`
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <RefreshCw className="h-8 w-8 animate-spin" />
      </div>
    )
  }

  return (
    <PageLoading page="dashboard">
      <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">Dashboard</h1>
          <p className="text-muted-foreground">
            AI API Gateway Usage Statistics & Analytics
          </p>
        </div>
        <div className="flex items-center space-x-2">
          <Select value={timeRange} onValueChange={setTimeRange}>
            <SelectTrigger className="w-[180px]">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="1h">Last 1 Hour</SelectItem>
              <SelectItem value="24h">Last 24 Hours</SelectItem>
              <SelectItem value="7d">Last 7 Days</SelectItem>
              <SelectItem value="30d">Last 30 Days</SelectItem>
            </SelectContent>
          </Select>
          <Button onClick={loadStatistics} variant="outline" size="sm">
            <RefreshCw className="h-4 w-4 mr-2" />
            Refresh
          </Button>
        </div>
      </div>

      {/* Key Metrics */}
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Total Requests</CardTitle>
            <Activity className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">
              {overview ? formatNumber(overview.totalRequests) : "0"}
            </div>
            <p className="text-xs text-muted-foreground">
              {requestUsage ? `${(requestUsage.successRate * 100).toFixed(1)}% success rate` : "No data"}
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Total Tokens</CardTitle>
            <Zap className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">
              {tokenUsage ? formatNumber(tokenUsage.totalTokens) : "0"}
            </div>
            <p className="text-xs text-muted-foreground">
              {tokenUsage ? `${formatNumber(tokenUsage.averageTokensPerRequest)} avg/request` : "No data"}
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Total Cost</CardTitle>
            <DollarSign className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">
              {costUsage ? formatCurrency(costUsage.totalCost) : "$0.00"}
            </div>
            <p className="text-xs text-muted-foreground">
              {costUsage ? `${formatCurrency(costUsage.averageCostPerRequest)} avg/request` : "No data"}
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Average Latency</CardTitle>
            <Timer className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">
              {latency ? formatLatency(latency.averageDurationMs) : "0ms"}
            </div>
            <p className="text-xs text-muted-foreground">
              {latency ? `P95: ${formatLatency(latency.p95DurationMs)}` : "No data"}
            </p>
          </CardContent>
        </Card>
      </div>

      <div className="grid gap-6 md:grid-cols-2">
        {/* Popular Models */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <TrendingUp className="h-5 w-5" />
              Popular Models
            </CardTitle>
            <CardDescription>
              Most requested AI models in selected period
            </CardDescription>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              {popularModels?.models.map((model, index) => (
                <div key={index} className="flex items-center justify-between">
                  <div className="flex items-center space-x-3">
                    <Badge variant={index === 0 ? "default" : "secondary"}>
                      #{index + 1}
                    </Badge>
                    <span className="font-medium">{model.modelKey}</span>
                  </div>
                  <div className="text-right">
                    <div className="text-sm font-medium">{formatNumber(model.requestCount)}</div>
                    <div className="text-xs text-muted-foreground">
                      {model.successRate !== undefined ? `${(model.successRate * 100).toFixed(1)}% success` : '0.0% success'}
                    </div>
                  </div>
                </div>
              ))}
              {!popularModels?.models.length && (
                <div className="text-center text-muted-foreground py-8">
                  No model usage data available
                </div>
              )}
            </div>
          </CardContent>
        </Card>

        {/* System Status */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <BarChart3 className="h-5 w-5" />
              System Status
            </CardTitle>
            <CardDescription>
              Current system performance and health
            </CardDescription>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              <div className="flex items-center justify-between">
                <span className="text-sm">Active Users</span>
                <span className="text-sm font-medium">
                  {overview ? overview.activeUsers : "0"}
                </span>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-sm">Error Rate</span>
                <div className="flex items-center space-x-2">
                  <span className="text-sm font-medium">
                    {errorRate ? `${errorRate.overallErrorRate.toFixed(2)}%` : "0%"}
                  </span>
                  {errorRate && errorRate.overallErrorRate > 0.05 && (
                    <AlertTriangle className="h-4 w-4 text-red-500" />
                  )}
                </div>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-sm">P99 Latency</span>
                <span className="text-sm font-medium">
                  {latency ? formatLatency(latency.p99DurationMs) : "0ms"}
                </span>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-sm">Peak Token Speed</span>
                <span className="text-sm font-medium">
                  {tokenUsage ? `${formatNumber(tokenUsage.averageTokensPerRequest)}/s` : "0/s"}
                </span>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-sm">Cost per Token</span>
                <span className="text-sm font-medium">
                  {costUsage ? formatCurrency(costUsage.averageCostPerToken) : "$0.00"}
                </span>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Usage Breakdown */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Target className="h-5 w-5" />
            Usage Breakdown
          </CardTitle>
          <CardDescription>
            Detailed usage statistics for selected period
          </CardDescription>
        </CardHeader>
        <CardContent>
          <div className="grid gap-6 md:grid-cols-3">
            <div className="text-center">
              <div className="text-3xl font-bold text-blue-600">
                {requestUsage ? formatNumber(requestUsage.successfulRequests) : "0"}
              </div>
              <p className="text-sm text-muted-foreground">Successful Requests</p>
            </div>
            <div className="text-center">
              <div className="text-3xl font-bold text-green-600">
                {tokenUsage ? formatNumber(tokenUsage.totalPromptTokens) : "0"}
              </div>
              <p className="text-sm text-muted-foreground">Input Tokens</p>
            </div>
            <div className="text-center">
              <div className="text-3xl font-bold text-purple-600">
                {tokenUsage ? formatNumber(tokenUsage.totalCompletionTokens) : "0"}
              </div>
              <p className="text-sm text-muted-foreground">Output Tokens</p>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
    </PageLoading>
  )
}

export default Dashboard

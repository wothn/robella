// API基础配置
const API_BASE_URL = '/api'

// API客户端类
class ApiClient {
  private baseURL: string

  constructor(baseURL: string = API_BASE_URL) {
    this.baseURL = baseURL
  }

  // 获取存储的访问令牌
  private getAccessToken(): string | null {
    return storage.getAccessToken()
  }

  // 通用请求方法
  private async request<T>(
    endpoint: string,
    options: RequestInit = {}
  ): Promise<T> {
    const url = `${this.baseURL}${endpoint}`

    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
      ...(options.headers as Record<string, string>),
    }

    // 为需要认证的请求添加访问令牌
    const token = this.getAccessToken()
    if (token) {
      headers['Authorization'] = `Bearer ${token}`
    }

    const config: RequestInit = {
      mode: 'cors',
      credentials: 'include',
      headers,
      ...options,
    }

    console.log('Making API request:', { url, method: config.method || 'GET' })

    try {
      const response = await fetch(url, config)

      console.log('API response:', { status: response.status, ok: response.ok })

      if (!response.ok) {
        const errorText = await response.text()
        console.error('API error response:', errorText)

        // 如果是401错误，尝试刷新令牌
        if (response.status === 401) {
          return this.handleTokenRefresh(endpoint, headers, config, url)
        }

        throw new Error(`HTTP error! status: ${response.status}, message: ${errorText}`)
      }

      // 检查响应内容类型
      const contentType = response.headers.get('content-type')
      if (contentType && contentType.includes('application/json')) {
        return await response.json()
      } else {
        return await response.text() as unknown as T
      }
    } catch (error) {
      console.error('API request failed:', error)
      throw error
    }
  }

  // GET请求
  async get<T>(endpoint: string): Promise<T> {
    return this.request<T>(endpoint, { method: 'GET' })
  }

  // POST请求
  async post<T>(endpoint: string, data?: unknown): Promise<T> {
    return this.request<T>(endpoint, {
      method: 'POST',
      body: JSON.stringify(data),
    })
  }

  // PUT请求
  async put<T>(endpoint: string, data?: unknown): Promise<T> {
    return this.request<T>(endpoint, {
      method: 'PUT',
      body: JSON.stringify(data),
    })
  }

  // DELETE请求
  async delete<T>(endpoint: string): Promise<T> {
    return this.request<T>(endpoint, { method: 'DELETE' })
  }

  // PATCH请求
  async patch<T>(endpoint: string, data?: unknown): Promise<T> {
    return this.request<T>(endpoint, {
      method: 'PATCH',
      body: JSON.stringify(data),
    })
  }

  // 用户登录
  async login(username: string, password: string): Promise<LoginResponse> {
    const loginRequest = {
      username,
      password
    }

    console.log('Attempting login with:', { username })

    return this.request<LoginResponse>('/users/login', {
      method: 'POST',
      body: JSON.stringify(loginRequest),
    })
  }

  // 获取当前用户信息
  async getCurrentUser(): Promise<User> {
    return this.get('/profile')
  }

  // 创建用户 (管理员)
  async createUser(userData: CreateUserRequest): Promise<User> {
    return this.post('/users', userData)
  }

  // 更新用户 (管理员)
  async updateUser(id: number, userData: Partial<User>): Promise<User> {
    return this.put(`/users/${id}`, userData)
  }

  // 删除用户 (管理员)
  async deleteUser(id: number): Promise<void> {
    return this.delete(`/users/${id}`)
  }

  // 更新当前用户资料
  async updateCurrentUser(userData: UserProfileUpdateRequest): Promise<User> {
    return this.put('/profile', userData)
  }

  // 删除当前用户
  async deleteCurrentUser(): Promise<void> {
    return this.delete('/profile')
  }

  // 修改当前用户密码
  async changePassword(currentPassword: string, newPassword: string): Promise<void> {
    return this.put(`/profile/password?currentPassword=${encodeURIComponent(currentPassword)}&newPassword=${encodeURIComponent(newPassword)}`)
  }

  // 激活用户 (管理员)
  async activateUser(id: number): Promise<User> {
    return this.put(`/users/${id}/active?active=true`)
  }

  // 停用用户 (管理员)
  async deactivateUser(id: number): Promise<User> {
    return this.put(`/users/${id}/active?active=false`)
  }

  // 获取用户信息 (管理员)
  async getUserById(id: number): Promise<User> {
    return this.get(`/users/${id}`)
  }

  // 获取所有用户 (管理员)
  async getAllUsers(active?: boolean): Promise<User[]> {
    const params = active !== undefined ? `?active=${active}` : ''
    return this.get(`/users${params}`)
  }

  // 获取活跃用户 (管理员)
  async getActiveUsers(): Promise<User[]> {
    return this.getAllUsers(true)
  }

  // 刷新令牌
  async refreshToken(): Promise<LoginResponse> {
    const response = await fetch(`${this.baseURL}/users/refresh`, {
      method: 'POST',
      credentials: 'include', // Important: include cookies
    })

    if (!response.ok) {
      storage.clearAuth()
      window.location.href = '/login'
      throw new Error('Failed to refresh token')
    }

    const data: LoginResponse = await response.json()
    // Only store accessToken in localStorage, refreshToken is already in HttpOnly cookie
    storage.setItem('accessToken', data.accessToken)
    return data
  }

  // 处理令牌刷新
  private async handleTokenRefresh<T>(
    endpoint: string,
    headers: Record<string, string>,
    config: RequestInit,
    url: string
  ): Promise<T> {
    // 检查是否正在刷新中，避免并发刷新
    if (storage.isRefreshing()) {
      // 等待刷新完成
      await this.waitForRefreshComplete()
      // 使用新token重试原请求
      return this.retryRequest<T>(endpoint, config)
    }

    try {
      storage.setRefreshing(true)

      // 尝试刷新令牌
      await this.refreshToken()

      // 使用新token重试原请求
      return this.retryRequest<T>(endpoint, config)
    } catch (error) {
      console.error('Token refresh failed:', error)
      storage.clearAuth()
      window.location.href = '/login'
      throw error
    } finally {
      storage.setRefreshing(false)
    }
  }

  // 等待刷新完成
  private async waitForRefreshComplete(): Promise<void> {
    const maxWaitTime = 10000 // 最多等待10秒
    const checkInterval = 100 // 每100ms检查一次
    let elapsedTime = 0

    return new Promise((resolve, reject) => {
      const checkIntervalId = setInterval(() => {
        if (!storage.isRefreshing()) {
          clearInterval(checkIntervalId)
          resolve()
        } else if (elapsedTime >= maxWaitTime) {
          clearInterval(checkIntervalId)
          reject(new Error('Token refresh timeout'))
        }
        elapsedTime += checkInterval
      }, checkInterval)
    })
  }

  // 重试请求
  private async retryRequest<T>(endpoint: string, originalConfig: RequestInit): Promise<T> {
    const config = {
      ...originalConfig,
      headers: {
        ...originalConfig.headers,
        'Authorization': `Bearer ${storage.getAccessToken()}`,
      },
    }

    const response = await fetch(`${this.baseURL}${endpoint}`, config)

    if (!response.ok) {
      const errorText = await response.text()
      throw new Error(`HTTP error! status: ${response.status}, message: ${errorText}`)
    }

    const contentType = response.headers.get('content-type')
    if (contentType && contentType.includes('application/json')) {
      return await response.json()
    } else {
      return await response.text() as unknown as T
    }
  }

  

  // GitHub OAuth登录
  async githubLogin(): Promise<void> {
    window.location.href = '/api/oauth/github/login'
  }

  // GitHub OAuth回调
  async githubCallback(code: string, state: string): Promise<LoginResponse> {
    return this.get(`/oauth/github/callback?code=${code}&state=${state}`)
  }

  // 用户登出
  async logout(): Promise<void> {
    return this.post('/users/logout', {})
  }

  // ================= Provider Management =================
  // 获取所有 Provider
  async getAllProviders(): Promise<Provider[]> {
    return this.get('/providers')
  }

  // 获取激活 Provider
  async getActiveProviders(): Promise<Provider[]> {
    return this.get('/providers/active')
  }

  // 根据 ID 获取 Provider
  async getProviderById(id: number): Promise<Provider> {
    return this.get(`/providers/${id}`)
  }

  // 创建 Provider
  async createProvider(data: CreateProviderRequest): Promise<Provider> {
    return this.post('/providers', data)
  }

  // 更新 Provider
  async updateProvider(id: number, data: UpdateProviderRequest): Promise<Provider> {
    return this.put(`/providers/${id}`, data)
  }

  // 删除 Provider
  async deleteProvider(id: number): Promise<void> {
    return this.delete(`/providers/${id}`)
  }

  // ================= Model Management =================
  // 获取所有模型
  async getModels(): Promise<Model[]> {
    return this.get('/models')
  }

  // 获取已发布的模型
  async getPublishedModels(): Promise<Model[]> {
    return this.get('/models/published')
  }

  // 根据ID获取模型
  async getModelById(id: number): Promise<Model> {
    return this.get(`/models/${id}`)
  }

  // 创建模型
  async createModel(data: CreateModelRequest): Promise<Model> {
    return this.post('/models', data)
  }

  // 更新模型
  async updateModel(id: number, data: UpdateModelRequest): Promise<Model> {
    return this.put(`/models/${id}`, data)
  }

  // 删除模型
  async deleteModel(id: number): Promise<void> {
    return this.delete(`/models/${id}`)
  }

  // 发布模型
  async publishModel(id: number): Promise<Model> {
    return this.put(`/models/${id}/publish`, {})
  }

  // 取消发布模型
  async unpublishModel(id: number): Promise<Model> {
    return this.put(`/models/${id}/unpublish`, {})
  }

  // 搜索模型
  async searchModels(keyword: string): Promise<Model[]> {
    return this.get(`/models/search?keyword=${encodeURIComponent(keyword)}`)
  }

  // 按能力筛选模型
  async getModelsByCapability(capability: string): Promise<Model[]> {
    return this.get(`/models/capability/${encodeURIComponent(capability)}`)
  }

  // ================= API Key Management =================
  // 获取用户所有 API Key
  async getUserApiKeys(): Promise<ApiKey[]> {
    return this.get('/api-keys')
  }

  // 创建新的 API Key
  async createApiKey(data: ApiKeyCreateRequest): Promise<ApiKey> {
    return this.post('/api-keys', data)
  }

  // 删除 API Key
  async deleteApiKey(id: number): Promise<void> {
    return this.delete(`/api-keys/${id}`)
  }

  // 切换 API Key 状态
  async toggleApiKeyStatus(id: number): Promise<ApiKey> {
    return this.patch(`/api-keys/${id}/toggle`, {})
  }

  // 获取模型关联的VendorModel
  async getVendorModelsByModelId(modelId: number): Promise<VendorModel[]> {
    return this.get(`/models/${modelId}/vendor-models`)
  }

  // 为模型添加VendorModel关联
  async addVendorModelToModel(modelId: number, vendorModelId: number): Promise<VendorModel> {
    return this.post(`/models/${modelId}/vendor-models/${vendorModelId}`, {})
  }

  // ================= Vendor Model Management =================
  // 获取某 Provider 的所有 Vendor Models
  async getVendorModelsByProviderId(providerId: number): Promise<VendorModel[]> {
    return this.get(`/providers/${providerId}/models`)
  }

  // 创建 Vendor Model
  async createVendorModel(providerId: number, data: CreateVendorModelRequest): Promise<VendorModel> {
    return this.post(`/providers/${providerId}/models`, data)
  }

  // 更新 Vendor Model
  async updateVendorModel(modelId: number, data: UpdateVendorModelRequest): Promise<VendorModel> {
    return this.put(`/providers/models/${modelId}`, data)
  }

  // 删除 Vendor Model
  async deleteVendorModel(modelId: number): Promise<void> {
    return this.delete(`/providers/models/${modelId}`)
  }

  // ================= Statistics API =================
  // 获取系统概览
  async getSystemOverview(startTime: string, endTime: string): Promise<SystemOverviewResponse> {
    return this.get(`/statistics/overview?startTime=${startTime}&endTime=${endTime}`)
  }

  // 获取用户概览
  async getUserOverview(userId: number, startTime: string, endTime: string): Promise<UserOverviewResponse> {
    return this.get(`/statistics/overview/user/${userId}?startTime=${startTime}&endTime=${endTime}`)
  }

  // 获取令牌使用情况
  async getTokenUsage(startTime: string, endTime: string, userId?: number): Promise<TokenUsageResponse> {
    const params = new URLSearchParams({
      startTime,
      endTime
    })
    if (userId) {
      params.append('userId', userId.toString())
    }
    return this.get(`/statistics/usage/tokens?${params}`)
  }

  // 获取成本使用情况
  async getCostUsage(startTime: string, endTime: string, userId?: number): Promise<CostUsageResponse> {
    const params = new URLSearchParams({
      startTime,
      endTime
    })
    if (userId) {
      params.append('userId', userId.toString())
    }
    return this.get(`/statistics/usage/costs?${params}`)
  }

  // 获取请求使用情况
  async getRequestUsage(startTime: string, endTime: string, userId?: number): Promise<RequestUsageResponse> {
    const params = new URLSearchParams({
      startTime,
      endTime
    })
    if (userId) {
      params.append('userId', userId.toString())
    }
    return this.get(`/statistics/usage/requests?${params}`)
  }

  // 获取延迟统计
  async getLatencyStats(startTime: string, endTime: string, userId?: number): Promise<LatencyStatsResponse> {
    const params = new URLSearchParams({
      startTime,
      endTime
    })
    if (userId) {
      params.append('userId', userId.toString())
    }
    return this.get(`/statistics/performance/latency?${params}`)
  }

  // 获取令牌速度统计
  async getTokenSpeedStats(startTime: string, endTime: string, userId?: number): Promise<TokenSpeedResponse> {
    const params = new URLSearchParams({
      startTime,
      endTime
    })
    if (userId) {
      params.append('userId', userId.toString())
    }
    return this.get(`/statistics/performance/tokens-per-second?${params}`)
  }

  // 获取模型流行度
  async getModelPopularity(startTime: string, endTime: string, limit: number = 10, userId?: number): Promise<ModelPopularityResponse> {
    const params = new URLSearchParams({
      startTime,
      endTime,
      limit: limit.toString()
    })
    if (userId) {
      params.append('userId', userId.toString())
    }
    return this.get(`/statistics/models/popularity?${params}`)
  }

  // 获取模型成本
  async getModelCosts(startTime: string, endTime: string, userId?: number): Promise<ModelCostResponse> {
    const params = new URLSearchParams({
      startTime,
      endTime
    })
    if (userId) {
      params.append('userId', userId.toString())
    }
    return this.get(`/statistics/models/costs?${params}`)
  }

  // 获取使用情况时间序列
  async getUsageTimeSeries(startTime: string, endTime: string, interval: string = 'hour', userId?: number): Promise<TimeSeriesResponse> {
    const params = new URLSearchParams({
      startTime,
      endTime,
      interval
    })
    if (userId) {
      params.append('userId', userId.toString())
    }
    return this.get(`/statistics/timeseries/usage?${params}`)
  }

  // 获取成本时间序列
  async getCostTimeSeries(startTime: string, endTime: string, interval: string = 'hour', userId?: number): Promise<TimeSeriesResponse> {
    const params = new URLSearchParams({
      startTime,
      endTime,
      interval
    })
    if (userId) {
      params.append('userId', userId.toString())
    }
    return this.get(`/statistics/timeseries/costs?${params}`)
  }

  // 获取错误率
  async getErrorRate(startTime: string, endTime: string, userId?: number): Promise<ErrorRateResponse> {
    const params = new URLSearchParams({
      startTime,
      endTime
    })
    if (userId) {
      params.append('userId', userId.toString())
    }
    return this.get(`/statistics/errors/rate?${params}`)
  }

  // 获取按模型分组的错误
  async getErrorsByModel(startTime: string, endTime: string, userId?: number): Promise<ErrorByModelResponse> {
    const params = new URLSearchParams({
      startTime,
      endTime
    })
    if (userId) {
      params.append('userId', userId.toString())
    }
    return this.get(`/statistics/errors/by-model?${params}`)
  }

}

// 导入类型定义
import type {
  User,
  LoginResponse,
  CreateUserRequest,
  UserProfileUpdateRequest,
  Provider,
  VendorModel,
  CreateProviderRequest,
  UpdateProviderRequest,
  CreateVendorModelRequest,
  UpdateVendorModelRequest,
  Model,
  CreateModelRequest,
  UpdateModelRequest,

  ApiKey,
  ApiKeyCreateRequest
} from '@/types'
import type {
  SystemOverviewResponse,
  UserOverviewResponse,
  TokenUsageResponse,
  CostUsageResponse,
  RequestUsageResponse,
  LatencyStatsResponse,
  TokenSpeedResponse,
  ModelPopularityResponse,
  ModelCostResponse,
  TimeSeriesResponse,
  ErrorRateResponse,
  ErrorByModelResponse
} from '@/types/statistics'
import { storage } from './storage'

// 创建API客户端实例
export const apiClient = new ApiClient()
export const api = apiClient

export default ApiClient

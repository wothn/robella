// API基础配置
const API_BASE_URL = '/api'

// API客户端类
class ApiClient {
  private baseURL: string

  constructor(baseURL: string = API_BASE_URL) {
    this.baseURL = baseURL
  }


  // 通用请求方法
  private async request<T>(
    endpoint: string,
    options: RequestInit = {},
    skipAuthRedirect: boolean = false
  ): Promise<T> {
    const url = `${this.baseURL}${endpoint}`

    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
      ...(options.headers as Record<string, string>),
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

      // 处理 401 未授权错误，重定向到登录页面
      if (response.status === 401 && !skipAuthRedirect) {
        console.log('Unauthorized, redirecting to login...')
        window.location.href = '/login'
        throw new Error('Unauthorized - redirecting to login')
      }

      if (!response.ok) {
        const errorText = await response.text()
        console.error('API error response:', errorText)


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
      // 如果是 401 重定向错误，直接抛出
      if (error instanceof Error && error.message === 'Unauthorized - redirecting to login') {
        throw error
      }
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
  async getCurrentUser(skipAuthRedirect: boolean = false): Promise<User> {
    return this.request('/profile', { method: 'GET' }, skipAuthRedirect)
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
  async deleteUser(id: number): Promise<boolean> {
    return this.delete(`/users/${id}`)
  }

  // 更新当前用户资料
  async updateCurrentUser(userData: UserProfileUpdateRequest): Promise<boolean> {
    return this.put('/profile', userData)
  }

  // 删除当前用户
  async deleteCurrentUser(): Promise<boolean> {
    return this.delete('/profile')
  }

  // 修改当前用户密码
  async changePassword(currentPassword: string, newPassword: string): Promise<boolean> {
    return this.put(`/profile/password?currentPassword=${encodeURIComponent(currentPassword)}&newPassword=${encodeURIComponent(newPassword)}`)
  }

  // 激活用户 (管理员)
  async activateUser(id: number): Promise<boolean> {
    return this.put(`/users/${id}/active?active=true`)
  }

  // 停用用户 (管理员)
  async deactivateUser(id: number): Promise<boolean> {
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





  

  // GitHub OAuth登录
  async githubLogin(): Promise<void> {
    window.location.href = '/api/oauth/github/login'
  }

  // GitHub OAuth回调
  async githubCallback(code: string, state: string): Promise<void> {
    await this.get<void>(`/oauth/github/callback?code=${code}&state=${state}`)
  }

  // 用户注册
  async register(username: string, email: string, password: string, confirmPassword: string, displayName?: string): Promise<LoginResponse> {
    const registerRequest = {
      username,
      email,
      password,
      confirmPassword,
      displayName
    }

    console.log('Attempting registration with:', { username, email })

    return this.request<LoginResponse>('/users/register', {
      method: 'POST',
      body: JSON.stringify(registerRequest),
    })
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
    return this.get(`/vendor-models/model/${modelId}`)
  }

  // ================= Vendor Model Management =================
  // 获取某 Provider 的所有 Vendor Models
  async getVendorModelsByProviderId(providerId: number): Promise<VendorModel[]> {
    return this.get(`/vendor-models/provider/${providerId}`)
  }

  private buildCreateVendorModelPayload(data: CreateVendorModelRequest) {
    const safeNumber = (value?: string | number | null): number | undefined => {
      if (value === undefined || value === null || value === '') {
        return undefined
      }
      const num = typeof value === 'number' ? value : parseFloat(value)
      return Number.isFinite(num) ? num : undefined
    }

    const vendorModel: Record<string, unknown> = {
      modelId: data.modelId ?? null,
      modelKey: data.modelKey ?? data.vendorModelKey,
      providerId: data.providerId,
      providerType: data.providerType,
      vendorModelName: data.vendorModelName,
      vendorModelKey: data.vendorModelKey,
      description: data.description ?? null,
      inputPerMillionTokens: safeNumber(data.inputPerMillionTokens),
      outputPerMillionTokens: safeNumber(data.outputPerMillionTokens),
      perRequestPrice: safeNumber(data.perRequestPrice),
      currency: data.currency ?? null,
      cachedInputPrice: safeNumber(data.cachedInputPrice),
      pricingStrategy: data.pricingStrategy,
      weight: (typeof data.weight === 'number' ? data.weight : safeNumber(data.weight)) ?? 5,
      enabled: data.enabled ?? true
    }

    const tiers =
      data.pricingStrategy === 'TIERED' && data.pricingTiers?.length
        ? data.pricingTiers.map(tier => ({
            ...tier,
            inputPerMillionTokens: safeNumber(tier.inputPerMillionTokens),
            outputPerMillionTokens: safeNumber(tier.outputPerMillionTokens),
            cachedInputPrice: safeNumber(tier.cachedInputPrice)
          }))
        : undefined

    return {
      vendorModel,
      pricingTiers: tiers
    }
  }

  private buildUpdateVendorModelPayload(id: number, data: UpdateVendorModelRequest) {
    const safeNumber = (value?: string | number | null): number | undefined => {
      if (value === undefined || value === null || value === '') {
        return undefined
      }
      const num = typeof value === 'number' ? value : parseFloat(value)
      return Number.isFinite(num) ? num : undefined
    }

    const vendorModel: Record<string, unknown> = { id }

    if ('modelId' in data) {
      vendorModel.modelId = data.modelId ?? null
    }

    if (data.modelKey !== undefined) {
      vendorModel.modelKey = data.modelKey
    }

    if (data.providerId !== undefined) {
      vendorModel.providerId = data.providerId
    }

    if (data.providerType !== undefined) {
      vendorModel.providerType = data.providerType
    }

    if (data.vendorModelName !== undefined) {
      vendorModel.vendorModelName = data.vendorModelName
    }

    if (data.vendorModelKey !== undefined) {
      vendorModel.vendorModelKey = data.vendorModelKey
    }

    if (data.description !== undefined) {
      vendorModel.description = data.description ?? null
    }

    if (data.pricingStrategy !== undefined) {
      vendorModel.pricingStrategy = data.pricingStrategy
    }

    const inputPrice = safeNumber(data.inputPerMillionTokens)
    if (inputPrice !== undefined) {
      vendorModel.inputPerMillionTokens = inputPrice
    }

    const outputPrice = safeNumber(data.outputPerMillionTokens)
    if (outputPrice !== undefined) {
      vendorModel.outputPerMillionTokens = outputPrice
    }

    const perRequestPrice = safeNumber(data.perRequestPrice)
    if (perRequestPrice !== undefined) {
      vendorModel.perRequestPrice = perRequestPrice
    }

    if (data.currency !== undefined) {
      vendorModel.currency = data.currency ?? null
    }

    const cachedPrice = safeNumber(data.cachedInputPrice)
    if (cachedPrice !== undefined) {
      vendorModel.cachedInputPrice = cachedPrice
    }

    const weightValue = typeof data.weight === 'number' ? data.weight : safeNumber(data.weight)
    if (weightValue !== undefined) {
      vendorModel.weight = weightValue
    }

    if (data.enabled !== undefined) {
      vendorModel.enabled = data.enabled
    }

    const tiers =
      data.pricingTiers !== undefined
        ? data.pricingTiers.map(tier => ({
            ...tier,
            inputPerMillionTokens: safeNumber(tier.inputPerMillionTokens),
            outputPerMillionTokens: safeNumber(tier.outputPerMillionTokens),
            cachedInputPrice: safeNumber(tier.cachedInputPrice)
          }))
        : undefined

    return {
      vendorModel,
      pricingTiers: tiers
    }
  }

  // 创建 Vendor Model
  async createVendorModel(data: CreateVendorModelRequest): Promise<VendorModel> {
    const payload = this.buildCreateVendorModelPayload(data)
    return this.post('/vendor-models', payload)
  }

  // 更新 Vendor Model
  async updateVendorModel(modelId: number, data: UpdateVendorModelRequest): Promise<VendorModel> {
    const payload = this.buildUpdateVendorModelPayload(modelId, data)
    return this.put(`/vendor-models/${modelId}`, payload)
  }

  // 删除 Vendor Model
  async deleteVendorModel(modelId: number): Promise<void> {
    return this.delete(`/vendor-models/${modelId}`)
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

// 创建API客户端实例
export const apiClient = new ApiClient()
export const api = apiClient

export default ApiClient
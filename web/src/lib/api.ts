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
    return localStorage.getItem('accessToken')
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
    if (token && !endpoint.includes('/login') && !endpoint.includes('/oauth/github')) {
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
          // 避免无限循环的刷新
          const isRefreshRequest = endpoint.includes('/users/refresh')
          const isAlreadyRefreshing = localStorage.getItem('isRefreshing') === 'true'
          
          if (isRefreshRequest || isAlreadyRefreshing) {
            // 如果是刷新请求失败或已经在刷新中，则清除token并重定向
            localStorage.removeItem('accessToken')
            localStorage.removeItem('refreshToken')
            localStorage.removeItem('isRefreshing')
            window.location.href = '/login'
            throw new Error('Token refresh failed')
          }

          // 标记正在刷新
          localStorage.setItem('isRefreshing', 'true')
          
          try {
            // 尝试刷新token
            const newTokens = await this.refreshToken()
            // 更新token并重试原请求
            localStorage.setItem('accessToken', newTokens.accessToken)
            localStorage.setItem('refreshToken', newTokens.refreshToken)
            localStorage.removeItem('isRefreshing')
            
            // 使用新token重试原请求
            const newHeaders = {
              ...headers,
              'Authorization': `Bearer ${newTokens.accessToken}`
            }
            
            const retryConfig: RequestInit = {
              ...config,
              headers: newHeaders
            }
            
            const retryResponse = await fetch(url, retryConfig)
            if (!retryResponse.ok) {
              throw new Error(`Retry failed with status: ${retryResponse.status}`)
            }
            
            const contentType = retryResponse.headers.get('content-type')
            if (contentType && contentType.includes('application/json')) {
              return await retryResponse.json()
            } else {
              return await retryResponse.text() as unknown as T
            }
          } catch (refreshError) {
            console.error('Token refresh failed:', refreshError)
            // 刷新失败，清除token并重定向
            localStorage.removeItem('accessToken')
            localStorage.removeItem('refreshToken')
            localStorage.removeItem('isRefreshing')
            window.location.href = '/login'
            throw new Error('Token refresh failed')
          }
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
    return this.get('/users/me')
  }

  // 获取用户信息
  async getUserByUsername(username: string): Promise<User> {
    return this.get(`/users/username/${username}`)
  }


  // 获取所有用户
  async getAllUsers(): Promise<User[]> {
    return this.get('/users')
  }

  // 获取活跃用户
  async getActiveUsers(): Promise<User[]> {
    return this.get('/users/active')
  }

  // 创建用户
  async createUser(userData: CreateUserRequest): Promise<User> {
    return this.post('/users', userData)
  }

  // 更新用户
  async updateUser(id: number, userData: Partial<User>): Promise<User> {
    return this.put(`/users/${id}`, userData)
  }

  // 删除用户
  async deleteUser(id: number): Promise<void> {
    return this.delete(`/users/${id}`)
  }

  // 激活用户
  async activateUser(id: number): Promise<User> {
    return this.put(`/users/${id}/activate`)
  }

  // 停用用户
  async deactivateUser(id: number): Promise<User> {
    return this.put(`/users/${id}/deactivate`)
  }

  // 用户登出
  async logout(): Promise<void> {
    return this.post('/users/logout')
  }

  // 刷新令牌
  async refreshToken(): Promise<LoginResponse> {
    const refreshToken = localStorage.getItem('refreshToken')
    if (!refreshToken) {
      console.error('No refresh token available in localStorage')
      throw new Error('No refresh token available')
    }
    
    console.log('Attempting to refresh token...')
    
    try {
      const response = await this.post<LoginResponse>('/users/refresh', { refreshToken })
      console.log('Token refresh successful')
      return response
    } catch (error) {
      console.error('Token refresh failed:', error)
      throw error
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

  // 按组织筛选模型
  async getModelsByOrganization(organization: string): Promise<Model[]> {
    return this.get(`/models/organization/${encodeURIComponent(organization)}`)
  }

  // 按能力筛选模型
  async getModelsByCapability(capability: string): Promise<Model[]> {
    return this.get(`/models/capability/${encodeURIComponent(capability)}`)
  }

  // 获取模型统计信息
  async getModelStats(): Promise<ModelStats> {
    return this.get('/models/stats')
  }

  // 获取组织模型数量
  async getModelCountByOrganization(organization: string): Promise<number> {
    return this.get(`/models/stats/organization/${encodeURIComponent(organization)}`)
  }

  // 检查模型名称是否存在
  async checkModelExists(name: string): Promise<boolean> {
    return this.get(`/models/exists/${encodeURIComponent(name)}`)
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

}

// 导入类型定义
import type {
  User,
  LoginResponse,
  CreateUserRequest,
  Provider,
  VendorModel,
  CreateProviderRequest,
  UpdateProviderRequest,
  CreateVendorModelRequest,
  UpdateVendorModelRequest,
  Model,
  CreateModelRequest,
  UpdateModelRequest,
  ModelStats
} from '../types'

// 创建API客户端实例
export const apiClient = new ApiClient()
export const api = apiClient

export default ApiClient

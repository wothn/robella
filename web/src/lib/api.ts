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
    options: RequestInit = {}
  ): Promise<T> {
    const url = `${this.baseURL}${endpoint}`
    
    // 获取token并添加到请求头
    const token = localStorage.getItem('accessToken')
    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
      ...options.headers as Record<string, string>,
    }
    
    // 如果有token且不是登录请求，添加Authorization头
    if (token && !endpoint.includes('/login')) {
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
  async post<T>(endpoint: string, data?: any): Promise<T> {
    return this.request<T>(endpoint, {
      method: 'POST',
      body: JSON.stringify(data),
    })
  }

  // PUT请求
  async put<T>(endpoint: string, data?: any): Promise<T> {
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
      password,
      rememberMe: false
    }

    console.log('Attempting login with:', { username })

    return this.request<LoginResponse>('/users/login', {
      method: 'POST',
      body: JSON.stringify(loginRequest),
    })
  }

  // 获取用户信息
  async getUserByUsername(username: string): Promise<any> {
    return this.get(`/users/username/${username}`)
  }

  // 获取所有用户
  async getAllUsers(): Promise<any[]> {
    return this.get('/users')
  }

  // 创建用户
  async createUser(userData: any): Promise<any> {
    return this.post('/users', userData)
  }

  // 更新用户
  async updateUser(id: number, userData: any): Promise<any> {
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

  // ================= Provider & Model Management =================
  // 获取所有 Provider
  async getProviders(): Promise<Provider[]> {
    return this.get('/providers')
  }

  // 获取激活 Provider
  async getActiveProviders(): Promise<Provider[]> {
    return this.get('/providers/active')
  }

  // 创建 Provider
  async createProvider(data: Partial<Provider>): Promise<Provider> {
    return this.post('/providers', data)
  }

  // 更新 Provider（全量）
  async updateProvider(id: number, data: Partial<Provider>): Promise<Provider> {
    return this.put(`/providers/${id}`, data)
  }

  // 局部更新 Provider
  async patchProvider(id: number, data: Partial<Provider>): Promise<Provider> {
    return this.request(`/providers/${id}`, { method: 'PATCH', body: JSON.stringify(data) })
  }

  // 删除 Provider
  async deleteProvider(id: number): Promise<void> {
    return this.delete(`/providers/${id}`)
  }

  // 获取某 Provider 的所有模型
  async getModelsByProvider(providerId: number): Promise<Model[]> {
    return this.get(`/providers/${providerId}/models`)
  }

  // 获取某 Provider 激活模型
  async getActiveModelsByProvider(providerId: number): Promise<Model[]> {
    return this.get(`/providers/${providerId}/models/active`)
  }

  // 创建模型
  async createModel(providerId: number, data: Partial<Model>): Promise<Model> {
    return this.post(`/providers/${providerId}/models`, data)
  }

  // 更新模型
  async updateModel(modelId: number, data: Partial<Model>): Promise<Model> {
    return this.put(`/providers/models/${modelId}`, data)
  }

  // 局部更新模型
  async patchModel(modelId: number, data: Partial<Model>): Promise<Model> {
    return this.request(`/providers/models/${modelId}`, { method: 'PATCH', body: JSON.stringify(data) })
  }

  // 删除模型
  async deleteModel(modelId: number): Promise<void> {
    return this.delete(`/providers/models/${modelId}`)
  }
}

// 创建API客户端实例
export const apiClient = new ApiClient()

// 导入类型定义
import type {
  User,
  LoginResponse,
  CreateUserRequest,
  Provider,
  Model,
  CreateProviderRequest,
  CreateModelRequest
} from '../types'

export default ApiClient

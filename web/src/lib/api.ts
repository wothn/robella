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
    
    const config: RequestInit = {
      mode: 'cors',
      credentials: 'include',
      headers: {
        'Content-Type': 'application/json',
        ...options.headers,
      },
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
}

// 创建API客户端实例
export const apiClient = new ApiClient()

// 导出类型定义
export interface User {
  id: number
  username: string
  email: string
  fullName: string
  avatar?: string | null
  phone?: string | null
  active: boolean
  role: string
  createdAt: string
  updatedAt: string
  lastLoginAt?: string | null
  emailVerified: boolean
  phoneVerified: boolean
}

export interface LoginResponse {
  user: User
  accessToken?: string
  refreshToken?: string
  expiresAt?: string
  loginTime: string
  message?: string
  sessionId?: string
}

export interface CreateUserRequest {
  username: string
  email: string
  password: string
  role?: string
}

export default ApiClient

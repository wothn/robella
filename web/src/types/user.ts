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
  sessionId: string
  loginTime: string
  message?: string
  success?: boolean
}

export interface CreateUserRequest {
  username: string
  email: string
  password: string
  role?: string
}
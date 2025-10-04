export interface User {
  id: number
  username: string
  email: string
  displayName: string
  avatar?: string | null
  active: boolean
  role: string
  credits?: number
  createdAt: string
  updatedAt: string
  lastLoginAt?: string | null
  githubId?: string | null
}

export function getRoleDisplayText(role: string): string {
  switch (role) {
    case 'GUEST':
      return 'Guest'
    case 'USER':
      return 'User'
    case 'ADMIN':
      return 'Admin'
    case 'ROOT':
      return 'Root'
    default:
      return 'User'
  }
}

export interface LoginResponse {
  success: boolean
  message: string
}

export interface CreateUserRequest {
  username: string
  email: string
  password: string
  displayName?: string
  role?: string
}

export interface UserProfileUpdateRequest {
  displayName?: string
}

export interface RegisterRequest {
  username: string;
  password: string;
  confirmPassword: string;
  displayName?: string;
}
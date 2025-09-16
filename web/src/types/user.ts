export interface User {
  id: number
  username: string
  email: string
  displayName: string
  avatar?: string | null
  phone?: string | null
  active: boolean
  role: number
  createdAt: string
  updatedAt: string
  lastLoginAt?: string | null
  githubId?: string | null
}

export function getRoleDisplayText(role: number): string {
  switch (role) {
    case 0:
      return 'Guest'
    case 1:
      return 'User'
    case 10:
      return 'Admin'
    case 100:
      return 'Root'
    default:
      return 'User'
  }
}

export interface LoginResponse {
  accessToken: string
  refreshToken: string
}

export interface CreateUserRequest {
  username: string
  email: string
  password: string
  displayName?: string
  role?: number
}

export interface RefreshTokenRequest {
  refreshToken: string
}
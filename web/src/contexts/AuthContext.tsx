import { createContext, useContext, useEffect, useState, type ReactNode } from 'react'
import { useNavigate } from 'react-router-dom'
import { apiClient } from '@/lib/api'
import type { User, LoginResponse } from '@/types/user'

interface AuthContextType {
  user: User | null
  loading: boolean
  login: (username: string, password: string) => Promise<void>
  logout: () => void
  githubLogin: () => void
  updateUser: (user: User) => void
  isAuthenticated: boolean
  validateSession: () => Promise<boolean>
}

const AuthContext = createContext<AuthContextType | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [loading, setLoading] = useState(true)
  const navigate = useNavigate()

  // Check if user is already logged in by getting current user
  useEffect(() => {
    validateCurrentSession()
  }, [])

  const validateCurrentSession = async () => {
    try {
      // Try to get current user with stored token
      const token = localStorage.getItem('accessToken')
      if (token) {
        const currentUser = await apiClient.getCurrentUser()
        setUser(currentUser)
      } else {
        setUser(null)
      }
    } catch (error) {
      console.error('Session validation failed:', error)
      setUser(null)
    } finally {
      setLoading(false)
    }
  }

  const validateSession = async (): Promise<boolean> => {
    try {
      const token = localStorage.getItem('accessToken')
      if (token) {
        const currentUser = await apiClient.getCurrentUser()
        setUser(currentUser)
        return true
      } else {
        console.log('No access token found in localStorage')
        setUser(null)
        return false
      }
    } catch (error) {
      console.error('Session validation failed:', error)
      // Try to refresh the token if validation fails
      try {
        console.log('Attempting to refresh token due to session validation failure')
        const response = await apiClient.refreshToken()
        localStorage.setItem('accessToken', response.accessToken)
        localStorage.setItem('refreshToken', response.refreshToken)
        localStorage.removeItem('isRefreshing')
        const currentUser = await apiClient.getCurrentUser()
        setUser(currentUser)
        console.log('Token refresh and session recovery successful')
        return true
      } catch (refreshError) {
        console.error('Token refresh failed:', refreshError)
        // Clear all tokens if refresh fails
        localStorage.removeItem('accessToken')
        localStorage.removeItem('refreshToken')
        localStorage.removeItem('isRefreshing')
        setUser(null)
        return false
      }
    }
  }

  const login = async (username: string, password: string) => {
    setLoading(true)
    try {
      const response: LoginResponse = await apiClient.login(username, password)
      
      // Store the tokens
      localStorage.setItem('accessToken', response.accessToken)
      localStorage.setItem('refreshToken', response.refreshToken)
      
      // Get the current user data
      const currentUser = await apiClient.getCurrentUser()
      setUser(currentUser)
      
      // Redirect to dashboard
      navigate('/')
    } catch (error) {
      console.error('Login failed:', error)
      throw error
    } finally {
      setLoading(false)
    }
  }

  const githubLogin = () => {
    const githubLoginUrl = '/api/oauth/github/login'
    window.location.href = githubLoginUrl
  }

  const logout = () => {
    // Clear stored data
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
    localStorage.removeItem('isRefreshing')
    setUser(null)
    navigate('/login')
  }

  const updateUser = (userData: User) => {
    setUser(userData)
  }

  const value: AuthContextType = {
    user,
    loading,
    login,
    logout,
    githubLogin,
    updateUser,
    isAuthenticated: !!user,
    validateSession
  }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider')
  }
  return context
}
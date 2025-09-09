import { createContext, useContext, useEffect, useState, type ReactNode } from 'react'
import { useNavigate } from 'react-router-dom'
import { apiClient } from '@/lib/api'
import type { User } from '@/types/user'

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

  // Check if user is already logged in by validating session
  useEffect(() => {
    validateCurrentSession()
  }, [])

  const validateCurrentSession = async () => {
    try {
      const response = await fetch('/api/auth/validate', {
        method: 'GET',
        credentials: 'include',
        headers: {
          'Content-Type': 'application/json',
        }
      })
      
      if (response.ok) {
        const data = await response.json()
        setUser(data.user)
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
      const response = await fetch('/api/auth/validate', {
        method: 'GET',
        credentials: 'include',
        headers: {
          'Content-Type': 'application/json',
        }
      })
      
      if (response.ok) {
        const data = await response.json()
        setUser(data.user)
        return true
      } else {
        setUser(null)
        return false
      }
    } catch (error) {
      console.error('Session validation failed:', error)
      setUser(null)
      return false
    }
  }

  const login = async (username: string, password: string) => {
    setLoading(true)
    try {
      const response = await apiClient.login(username, password)
      
      if (response.user) {
        setUser(response.user)
        
        // Redirect to dashboard
        navigate('/')
      } else {
        throw new Error('Login failed: No user data received')
      }
    } catch (error) {
      console.error('Login failed:', error)
      throw error
    } finally {
      setLoading(false)
    }
  }

  const githubLogin = () => {
    const githubLoginUrl = '/oauth2/authorization/github'
    window.location.href = githubLoginUrl
  }

  const logout = async () => {
    try {
      await fetch('/api/auth/logout', {
        method: 'POST',
        credentials: 'include',
        headers: {
          'Content-Type': 'application/json',
        }
      })
    } catch (error) {
      console.error('Logout failed:', error)
    } finally {
      setUser(null)
      navigate('/login')
    }
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
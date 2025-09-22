import { create } from 'zustand'
import { devtools, persist } from 'zustand/middleware'
import { apiClient } from '@/lib/api'
import { storage } from '@/lib/storage'
import type { User, LoginResponse } from '@/types/user'

interface AuthState {
  user: User | null
  loading: boolean
  login: (username: string, password: string) => Promise<void>
  logout: () => void
  githubLogin: () => void
  updateUser: (user: User) => void
  validateSession: () => Promise<boolean>
  setLoading: (loading: boolean) => void
}

export const useAuthStore = create<AuthState>()(
  devtools(
    persist(
      (set, get) => ({
        user: null,
        loading: true,

        validateSession: async (): Promise<boolean> => {
          try {
            const token = storage.getAccessToken()
            if (token) {
              const currentUser = await apiClient.getCurrentUser()
              set({ user: currentUser })
              return true
            } else {
              console.log('No access token found in localStorage')
              set({ user: null })
              return false
            }
          } catch (error) {
            console.error('Session validation failed:', error)
            set({ user: null })
            return false
          }
        },

        login: async (username: string, password: string) => {
          set({ loading: true })
          try {
            const response: LoginResponse = await apiClient.login(username, password)
            storage.setItem('accessToken', response.accessToken)
            const currentUser = await apiClient.getCurrentUser()
            set({ user: currentUser, loading: false })
            window.location.href = '/'
          } catch (error) {
            console.error('Login failed:', error)
            set({ loading: false })
            throw error
          }
        },

        githubLogin: () => {
          const githubLoginUrl = '/api/oauth/github/login'
          window.location.href = githubLoginUrl
        },

        logout: async () => {
          try {
            await apiClient.logout()
          } catch (error) {
            console.error('Logout API call failed:', error)
          } finally {
            storage.clearAuth()
            set({ user: null, loading: false })
            window.location.href = '/login'
          }
        },

        updateUser: (userData: User) => {
          set({ user: userData })
        },

        setLoading: (loading: boolean) => {
          set({ loading })
        }
      }),
      {
        name: 'auth-storage',
        partialize: (state) => ({ user: state.user }),
        onRehydrateStorage: () => (state) => {
          if (state) {
            state.validateSession()
          }
        }
      }
    ),
    { name: 'auth-store' }
  )
)
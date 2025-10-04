import {create} from 'zustand'
import {devtools, persist} from 'zustand/middleware'
import {apiClient} from '@/lib/api'
import {storage} from '@/lib/storage'
import {useLoadingStore, withGlobalLoading} from '@/stores/loading-store'
import type {User} from '@/types/user'

interface AuthState {
  user: User | null
  login: (username: string, password: string) => Promise<void>
  register: (username: string, password: string, confirmPassword: string, displayName?: string) => Promise<void>
  logout: () => void
  githubLogin: () => void
  updateUser: (user: User) => void
  validateSession: () => Promise<boolean>
}

export const useAuthStore = create<AuthState>()(
  devtools(
    persist(
      (set) => ({
        user: null,

        validateSession: async (): Promise<boolean> => {
          return withGlobalLoading(async () => {
            try {
              const currentUser = await apiClient.getCurrentUser(true)
              set({ user: currentUser })
              return true
            } catch (error) {
              console.error('Session validation failed:', error)
              set({ user: null })
              return false
            }
          })
        },

        login: async (username: string, password: string) => {
          return withGlobalLoading(async () => {
            try {
              await apiClient.login(username, password)
              const currentUser = await apiClient.getCurrentUser()
              set({ user: currentUser })
              window.location.href = '/'
            } catch (error) {
              console.error('Login failed:', error)
              throw error
            }
          })
        },

        register: async (username: string, password: string, confirmPassword: string, displayName?: string) => {
          return withGlobalLoading(async () => {
            try {
              await apiClient.register(username, password, confirmPassword, displayName)
              const currentUser = await apiClient.getCurrentUser()
              set({ user: currentUser })
              window.location.href = '/'
            } catch (error) {
              console.error('Registration failed:', error)
              throw error
            }
          })
        },

        githubLogin: () => {
            window.location.href = '/api/oauth/github/login'
        },

        logout: async () => {
          try {
            await apiClient.logout()
          } catch (error) {
            console.error('Logout API call failed:', error)
          } finally {
            storage.clearAuth()
            set({ user: null })
            useLoadingStore.getState().setGlobalLoading(false)
            window.location.href = '/login'
          }
        },

        updateUser: (userData: User) => {
          set({ user: userData })
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
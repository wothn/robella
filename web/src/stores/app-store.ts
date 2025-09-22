import { create } from 'zustand'
import { devtools } from 'zustand/middleware'
import type { Provider, Model } from '@/types'

interface AppState {
  providers: Provider[]
  models: Record<number, Model[]>
  currentProviderId: number | null
  loading: boolean
  error: string | null

  // Actions
  setProviders: (providers: Provider[]) => void
  setModels: (providerId: number, models: Model[]) => void
  setCurrentProvider: (providerId: number | null) => void
  setLoading: (loading: boolean) => void
  setError: (error: string | null) => void
  addProvider: (provider: Provider) => void
  updateProvider: (provider: Provider) => void
  deleteProvider: (providerId: number) => void
  addModel: (providerId: number, model: Model) => void
  updateModel: (model: Model) => void
  deleteModel: (modelId: number) => void
  clearError: () => void
}

export const useAppStore = create<AppState>()(
  devtools(
    (set, get) => ({
      providers: [],
      models: {},
      currentProviderId: null,
      loading: false,
      error: null,

      setProviders: (providers: Provider[]) => {
        set({ providers, loading: false, error: null })
      },

      setModels: (providerId: number, models: Model[]) => {
        set((state) => ({
          models: {
            ...state.models,
            [providerId]: models
          }
        }))
      },

      setCurrentProvider: (providerId: number | null) => {
        set({ currentProviderId: providerId })
      },

      setLoading: (loading: boolean) => {
        set({ loading })
      },

      setError: (error: string | null) => {
        set({ error, loading: false })
      },

      clearError: () => {
        set({ error: null })
      },

      addProvider: (provider: Provider) => {
        set((state) => ({
          providers: [...state.providers, provider]
        }))
      },

      updateProvider: (provider: Provider) => {
        set((state) => ({
          providers: state.providers.map(p =>
            p.id === provider.id ? provider : p
          )
        }))
      },

      deleteProvider: (providerId: number) => {
        set((state) => {
          const newModels = { ...state.models }
          delete newModels[providerId]
          return {
            providers: state.providers.filter(p => p.id !== providerId),
            models: newModels,
            currentProviderId: state.currentProviderId === providerId ? null : state.currentProviderId
          }
        })
      },

      addModel: (providerId: number, model: Model) => {
        set((state) => ({
          models: {
            ...state.models,
            [providerId]: [...(state.models[providerId] || []), model]
          }
        }))
      },

      updateModel: (model: Model) => {
        set((state) => {
          const updatedModels = { ...state.models }
          for (const providerId in updatedModels) {
            updatedModels[providerId] = updatedModels[providerId].map(m =>
              m.id === model.id ? model : m
            )
          }
          return { models: updatedModels }
        })
      },

      deleteModel: (modelId: number) => {
        set((state) => {
          const finalModels = { ...state.models }
          for (const providerId in finalModels) {
            finalModels[providerId] = finalModels[providerId].filter(m =>
              m.id !== modelId
            )
          }
          return { models: finalModels }
        })
      }
    }),
    { name: 'app-store' }
  )
)
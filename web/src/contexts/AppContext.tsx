import React, { createContext, useContext, useReducer, ReactNode } from 'react'
import type { Provider, Model } from '../types'

interface AppState {
  providers: Provider[]
  models: Record<number, Model[]>
  currentProviderId: number | null
  loading: boolean
  error: string | null
}

type AppAction =
  | { type: 'SET_PROVIDERS'; payload: Provider[] }
  | { type: 'SET_MODELS'; payload: { providerId: number; models: Model[] } }
  | { type: 'SET_CURRENT_PROVIDER'; payload: number | null }
  | { type: 'SET_LOADING'; payload: boolean }
  | { type: 'SET_ERROR'; payload: string | null }
  | { type: 'ADD_PROVIDER'; payload: Provider }
  | { type: 'UPDATE_PROVIDER'; payload: Provider }
  | { type: 'DELETE_PROVIDER'; payload: number }
  | { type: 'ADD_MODEL'; payload: { providerId: number; model: Model } }
  | { type: 'UPDATE_MODEL'; payload: Model }
  | { type: 'DELETE_MODEL'; payload: number }

const initialState: AppState = {
  providers: [],
  models: {},
  currentProviderId: null,
  loading: false,
  error: null
}

function appReducer(state: AppState, action: AppAction): AppState {
  switch (action.type) {
    case 'SET_PROVIDERS':
      return {
        ...state,
        providers: action.payload,
        loading: false
      }
    
    case 'SET_MODELS':
      return {
        ...state,
        models: {
          ...state.models,
          [action.payload.providerId]: action.payload.models
        }
      }
    
    case 'SET_CURRENT_PROVIDER':
      return {
        ...state,
        currentProviderId: action.payload
      }
    
    case 'SET_LOADING':
      return {
        ...state,
        loading: action.payload
      }
    
    case 'SET_ERROR':
      return {
        ...state,
        error: action.payload,
        loading: false
      }
    
    case 'ADD_PROVIDER':
      return {
        ...state,
        providers: [...state.providers, action.payload]
      }
    
    case 'UPDATE_PROVIDER':
      return {
        ...state,
        providers: state.providers.map(p => 
          p.id === action.payload.id ? action.payload : p
        )
      }
    
    case 'DELETE_PROVIDER':
      const newModels = { ...state.models }
      delete newModels[action.payload]
      return {
        ...state,
        providers: state.providers.filter(p => p.id !== action.payload),
        models: newModels,
        currentProviderId: state.currentProviderId === action.payload ? null : state.currentProviderId
      }
    
    case 'ADD_MODEL':
      const providerId = action.payload.providerId
      return {
        ...state,
        models: {
          ...state.models,
          [providerId]: [...(state.models[providerId] || []), action.payload.model]
        }
      }
    
    case 'UPDATE_MODEL':
      const updatedModels = { ...state.models }
      for (const providerId in updatedModels) {
        updatedModels[providerId] = updatedModels[providerId].map(m =>
          m.id === action.payload.id ? action.payload : m
        )
      }
      return {
        ...state,
        models: updatedModels
      }
    
    case 'DELETE_MODEL':
      const finalModels = { ...state.models }
      for (const providerId in finalModels) {
        finalModels[providerId] = finalModels[providerId].filter(m =>
          m.id !== action.payload
        )
      }
      return {
        ...state,
        models: finalModels
      }
    
    default:
      return state
  }
}

const AppContext = createContext<{
  state: AppState
  dispatch: React.Dispatch<AppAction>
} | null>(null)

export function AppProvider({ children }: { children: ReactNode }) {
  const [state, dispatch] = useReducer(appReducer, initialState)

  return (
    <AppContext.Provider value={{ state, dispatch }}>
      {children}
    </AppContext.Provider>
  )
}

export function useAppContext() {
  const context = useContext(AppContext)
  if (!context) {
    throw new Error('useAppContext must be used within an AppProvider')
  }
  return context
}
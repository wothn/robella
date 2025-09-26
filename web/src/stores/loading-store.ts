import { create } from 'zustand'
import { devtools } from 'zustand/middleware'

export interface LoadingState {
  // 全局loading (用于页面切换、认证等)
  global: boolean

  // 页面级loading (用于数据获取)
  pages: {
    [key: string]: boolean
  }

  // 组件级loading (用于具体操作)
  components: {
    [key: string]: boolean
  }

  // 操作级loading (用于按钮、表单等)
  actions: {
    [key: string]: boolean
  }

  // 计算属性
  isLoading: boolean
  getPageLoading: (page: string) => boolean
  getComponentLoading: (component: string) => boolean
  getActionLoading: (action: string) => boolean

  // Actions
  setGlobalLoading: (loading: boolean) => void
  setPageLoading: (page: string, loading: boolean) => void
  setComponentLoading: (component: string, loading: boolean) => void
  setActionLoading: (action: string, loading: boolean) => void
  clearPageLoading: (page: string) => void
  clearComponentLoading: (component: string) => void
  clearActionLoading: (action: string) => void
  clearAllLoading: () => void
}

export const useLoadingStore = create<LoadingState>()(
  devtools(
    (set, get) => ({
      global: false,
      pages: {},
      components: {},
      actions: {},

      // 计算是否有任何loading状态
      get isLoading() {
        const state = get()
        return (
          state.global ||
          Object.values(state.pages).some(Boolean) ||
          Object.values(state.components).some(Boolean) ||
          Object.values(state.actions).some(Boolean)
        )
      },

      // 获取页面loading状态
      getPageLoading: (page: string) => {
        return get().pages[page] || false
      },

      // 获取组件loading状态
      getComponentLoading: (component: string) => {
        return get().components[component] || false
      },

      // 获取操作loading状态
      getActionLoading: (action: string) => {
        return get().actions[action] || false
      },

      // 设置全局loading
      setGlobalLoading: (loading: boolean) => {
        set({ global: loading })
      },

      // 设置页面loading
      setPageLoading: (page: string, loading: boolean) => {
        set((state) => ({
          pages: {
            ...state.pages,
            [page]: loading
          }
        }))
      },

      // 设置组件loading
      setComponentLoading: (component: string, loading: boolean) => {
        set((state) => ({
          components: {
            ...state.components,
            [component]: loading
          }
        }))
      },

      // 设置操作loading
      setActionLoading: (action: string, loading: boolean) => {
        set((state) => ({
          actions: {
            ...state.actions,
            [action]: loading
          }
        }))
      },

      // 清除页面loading
      clearPageLoading: (page: string) => {
        set((state) => {
          const newPages = { ...state.pages }
          delete newPages[page]
          return { pages: newPages }
        })
      },

      // 清除组件loading
      clearComponentLoading: (component: string) => {
        set((state) => {
          const newComponents = { ...state.components }
          delete newComponents[component]
          return { components: newComponents }
        })
      },

      // 清除操作loading
      clearActionLoading: (action: string) => {
        set((state) => {
          const newActions = { ...state.actions }
          delete newActions[action]
          return { actions: newActions }
        })
      },

      // 清除所有loading
      clearAllLoading: () => {
        set({
          global: false,
          pages: {},
          components: {},
          actions: {}
        })
      }
    }),
    { name: 'loading-store' }
  )
)

// Loading hooks for easy usage
export function useGlobalLoading() {
  return {
    loading: useLoadingStore((state) => state.global),
    setLoading: useLoadingStore((state) => state.setGlobalLoading)
  }
}

export function usePageLoading(page: string) {
  return {
    loading: useLoadingStore((state) => state.getPageLoading(page)),
    setLoading: useLoadingStore((state) => state.setPageLoading)
  }
}

export function useComponentLoading(component: string) {
  return {
    loading: useLoadingStore((state) => state.getComponentLoading(component)),
    setLoading: useLoadingStore((state) => state.setComponentLoading)
  }
}

export function useActionLoading(action: string) {
  return {
    loading: useLoadingStore((state) => state.getActionLoading(action)),
    setLoading: useLoadingStore((state) => state.setActionLoading)
  }
}

// Loading utilities for async operations
export function withLoading<T>(
  setLoading: (loading: boolean) => void,
  asyncFn: () => Promise<T>
): Promise<T> {
  setLoading(true)
  try {
    return asyncFn()
  } finally {
    setLoading(false)
  }
}

export function withGlobalLoading<T>(asyncFn: () => Promise<T>): Promise<T> {
  return withLoading(
    useLoadingStore.getState().setGlobalLoading,
    asyncFn
  )
}

export function withPageLoading<T>(page: string, asyncFn: () => Promise<T>): Promise<T> {
  return withLoading(
    useLoadingStore.getState().setPageLoading.bind(null, page),
    asyncFn
  )
}

export function withComponentLoading<T>(component: string, asyncFn: () => Promise<T>): Promise<T> {
  return withLoading(
    useLoadingStore.getState().setComponentLoading.bind(null, component),
    asyncFn
  )
}

export function withActionLoading<T>(action: string, asyncFn: () => Promise<T>): Promise<T> {
  return withLoading(
    useLoadingStore.getState().setActionLoading.bind(null, action),
    asyncFn
  )
}
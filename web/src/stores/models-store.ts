import { create } from 'zustand'
import { devtools } from 'zustand/middleware'
import type { ModelFilters, Model } from '@/types/model'
import type { ModelTabType } from '@/constants/model-constants'

// 删除函数类型
type DeleteModelFunction = (id: number) => Promise<void>

interface ModelsState {
  // UI状态
  searchQuery: string
  filters: ModelFilters
  activeTab: ModelTabType
  showFilters: boolean
  showCreateDialog: boolean
  selectedModelId: number | null

  // 对话框状态
  editingModel: Model | null
  configModel: Model | null
  deleteModelId: number | null
  copiedModelKey: string | null
  deleteModelFunction: DeleteModelFunction | null

  // Actions
  setSearchQuery: (query: string) => void
  setFilters: (filters: ModelFilters) => void
  setActiveTab: (tab: ModelTabType) => void
  setShowFilters: (show: boolean) => void
  setShowCreateDialog: (show: boolean) => void
  setSelectedModelId: (id: number | null) => void

  setEditingModel: (model: Model | null) => void
  setConfigModel: (model: Model | null) => void
  setDeleteModelId: (id: number | null) => void
  setCopiedModelKey: (key: string | null) => void

  resetFilters: () => void
  resetDialogs: () => void
  setDeleteModelFunction: (fn: DeleteModelFunction | null) => void
}

const initialState = {
  searchQuery: '',
  filters: {},
  activeTab: 'all' as ModelTabType,
  showFilters: false,
  showCreateDialog: false,
  selectedModelId: null,
  editingModel: null,
  configModel: null,
  deleteModelId: null,
  copiedModelKey: null,
  deleteModelFunction: null as DeleteModelFunction | null,
}

export const useModelsStore = create<ModelsState>()(
  devtools(
    (set) => ({
      ...initialState,

      setSearchQuery: (searchQuery) => set({ searchQuery }),
      setFilters: (filters) => set({ filters }),
      setActiveTab: (activeTab) => set({ activeTab }),
      setShowFilters: (showFilters) => set({ showFilters }),
      setShowCreateDialog: (showCreateDialog) => set({ showCreateDialog }),
      setSelectedModelId: (selectedModelId) => set({ selectedModelId }),

      setEditingModel: (editingModel) => set({ editingModel }),
      setConfigModel: (configModel) => set({ configModel }),
      setDeleteModelId: (deleteModelId) => set({ deleteModelId }),
      setCopiedModelKey: (copiedModelKey) => set({ copiedModelKey }),

      resetFilters: () => set({ filters: {} }),
      resetDialogs: () => set({
        editingModel: null,
        configModel: null,
        deleteModelId: null,
        copiedModelKey: null,
      }),
      setDeleteModelFunction: (deleteModelFunction) => set({ deleteModelFunction })
    }),
    { name: 'models-store' }
  )
)
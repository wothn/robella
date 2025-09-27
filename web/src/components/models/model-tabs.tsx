import { memo } from 'react'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { BarChart3 } from 'lucide-react'
import { ModelListContainer } from './model-list-container'
import type { Model, ModelFilters as ModelFiltersType } from '@/types/model'
import type { ModelTabType } from '@/constants/model-constants'

interface ModelTabsProps {
  models: Model[]
  publishedModels: Model[]
  loading: boolean
  error: string | null
  activeTab: ModelTabType
  filters: ModelFiltersType
  bindings: Record<number, number>
  copiedModelKey: string | null
  onTabChange: (value: string) => void
  onRefresh: () => void
  onViewDetails: (modelId: number) => void
  onEdit: (model: Model) => void
  onTogglePublish: (model: Model) => void
  onConfigure: (model: Model) => void
  onDelete: (model: Model) => void
  onCopyModelKey: (modelKey: string, e: React.MouseEvent) => void
}

export const ModelTabs = memo(({
  models,
  publishedModels,
  loading,
  error,
  activeTab,
  filters,
  bindings,
  copiedModelKey,
  onTabChange,
  onRefresh,
  onViewDetails,
  onEdit,
  onTogglePublish,
  onConfigure,
  onDelete,
  onCopyModelKey
}: ModelTabsProps) => {
  return (
    <Tabs value={activeTab} onValueChange={onTabChange}>
      <TabsList className="grid w-full grid-cols-3">
        <TabsTrigger value="all" className="flex items-center gap-2">
          <BarChart3 className="h-4 w-4" />
          全部模型
        </TabsTrigger>
        <TabsTrigger value="published" className="flex items-center gap-2">
          已发布
        </TabsTrigger>
        <TabsTrigger value="draft" className="flex items-center gap-2">
          草稿
        </TabsTrigger>
      </TabsList>

      <TabsContent value="all" className="mt-6">
        <ModelListContainer
          models={models}
          publishedModels={publishedModels}
          loading={loading}
          error={error}
          activeTab={activeTab}
          filters={filters}
          bindings={bindings}
          copiedModelKey={copiedModelKey}
          onRefresh={onRefresh}
          onViewDetails={onViewDetails}
          onEdit={onEdit}
          onTogglePublish={onTogglePublish}
          onConfigure={onConfigure}
          onDelete={onDelete}
          onCopyModelKey={onCopyModelKey}
        />
      </TabsContent>

      <TabsContent value="published" className="mt-6">
        <ModelListContainer
          models={models}
          publishedModels={publishedModels}
          loading={loading}
          error={error}
          activeTab={activeTab}
          filters={filters}
          bindings={bindings}
          copiedModelKey={copiedModelKey}
          onRefresh={onRefresh}
          onViewDetails={onViewDetails}
          onEdit={onEdit}
          onTogglePublish={onTogglePublish}
          onConfigure={onConfigure}
          onDelete={onDelete}
          onCopyModelKey={onCopyModelKey}
        />
      </TabsContent>

      <TabsContent value="draft" className="mt-6">
        <ModelListContainer
          models={models}
          publishedModels={publishedModels}
          loading={loading}
          error={error}
          activeTab={activeTab}
          filters={filters}
          bindings={bindings}
          copiedModelKey={copiedModelKey}
          onRefresh={onRefresh}
          onViewDetails={onViewDetails}
          onEdit={onEdit}
          onTogglePublish={onTogglePublish}
          onConfigure={onConfigure}
          onDelete={onDelete}
          onCopyModelKey={onCopyModelKey}
        />
      </TabsContent>
    </Tabs>
  )
})

ModelTabs.displayName = 'ModelTabs'
import { memo, useMemo } from 'react'
import { Card, CardContent } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import { ModelCard } from './model-card'
import type { Model, ModelFilters as ModelFiltersType } from '@/types/model'
import type { ModelTabType } from '@/constants/model-constants'

interface ModelListContainerProps {
  models: Model[]
  publishedModels: Model[]
  loading: boolean
  error: string | null
  activeTab: ModelTabType
  filters: ModelFiltersType
  bindings: Record<number, number>
  copiedModelKey: string | null
  onRefresh: () => void
  onViewDetails: (modelId: number) => void
  onEdit: (model: Model) => void
  onTogglePublish: (model: Model) => void
  onConfigure: (model: Model) => void
  onDelete: (model: Model) => void
  onCopyModelKey: (modelKey: string, e: React.MouseEvent) => void
}

export const ModelListContainer = memo(({
  models,
  publishedModels,
  loading,
  error,
  activeTab,
  filters,
  bindings,
  copiedModelKey,
  onRefresh,
  onViewDetails,
  onEdit,
  onTogglePublish,
  onConfigure,
  onDelete,
  onCopyModelKey
}: ModelListContainerProps) => {
  // 优化：使用useMemo避免重复计算
  const filteredData = useMemo(() => {
    let data = models

    // 根据tab筛选
    if (activeTab === 'published') {
      data = publishedModels
    } else if (activeTab === 'draft') {
      data = models.filter(model => !model.published)
    }

    // 根据filters筛选
    if (filters.organization) {
      data = data.filter(model => model.organization === filters.organization)
    }

    if (filters.capability) {
      data = data.filter(model =>
        model.capabilities?.includes(filters.capability as import('@/types/model').ModelCapability)
      )
    }

    if (filters.published !== undefined) {
      data = data.filter(model => model.published === filters.published)
    }

    return data
  }, [models, publishedModels, activeTab, filters])

  // 加载状态
  if (loading) {
    return (
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
        {Array.from({ length: 6 }).map((_, i) => (
          <Card key={i}>
            <CardContent className="p-6">
              <div className="space-y-4">
                <div className="flex justify-between items-start">
                  <div className="space-y-2">
                    <Skeleton className="h-5 w-[150px]" />
                    <Skeleton className="h-4 w-[100px]" />
                  </div>
                  <Skeleton className="h-8 w-8 rounded" />
                </div>
                <Skeleton className="h-4 w-full" />
                <Skeleton className="h-4 w-3/4" />
                <div className="flex gap-2">
                  <Skeleton className="h-6 w-16" />
                  <Skeleton className="h-6 w-16" />
                </div>
              </div>
            </CardContent>
          </Card>
        ))}
      </div>
    )
  }

  // 错误状态
  if (error) {
    return (
      <Card>
        <CardContent className="flex flex-col items-center justify-center py-12">
          <p className="text-red-500 mb-4">{error}</p>
          <Button onClick={onRefresh}>重试</Button>
        </CardContent>
      </Card>
    )
  }

  // 空状态
  if (filteredData.length === 0) {
    return (
      <Card>
        <CardContent className="flex flex-col items-center justify-center py-12">
          <p className="text-muted-foreground mb-4">
            {Object.keys(filters).some(key => filters[key as keyof ModelFiltersType])
              ? '没有符合筛选条件的模型'
              : '暂无模型数据'
            }
          </p>
          <Button onClick={onRefresh}>刷新</Button>
        </CardContent>
      </Card>
    )
  }

  return (
    <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
      {filteredData.map((model) => (
        <div
          key={model.id}
          onClick={(e) => {
            const target = e.target as HTMLElement
            if (target.closest('[data-no-card-click="true"]')) return
            onViewDetails(model.id)
          }}
        >
          <ModelCard
            model={model}
            bindingsCount={bindings[model.id] || 0}
            onViewDetails={onViewDetails}
            onEdit={onEdit}
            onTogglePublish={onTogglePublish}
            onConfigure={onConfigure}
            onDelete={onDelete}
            onCopyModelKey={onCopyModelKey}
            copiedModelKey={copiedModelKey}
          />
        </div>
      ))}
    </div>
  )
})

ModelListContainer.displayName = 'ModelListContainer'
'use client'

import { useCallback, useEffect } from 'react'
import { Plus, Search } from 'lucide-react'
import { PageHeader } from '@/components/layout/page-header'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { ModelFilters } from '@/components/models/model-filters'
import { ModelTabs } from '@/components/models/model-tabs'
import { ModelFormDialog } from '@/components/models/model-form-dialog'
import { ModelDetailDialog } from '@/components/models/model-detail-dialog'
import { ModelConfigDialog } from '@/components/models/model-config-dialog'
import { ModelDeleteDialog } from '@/components/models/model-delete-dialog'
import { useModels } from '@/hooks/use-models'
import { useDebouncedValue } from '@/hooks/useDebouncedValue'
import { useModelBindings } from '@/hooks/use-model-bindings'
import { useModelsStore } from '@/stores/models-store'
import { toast } from 'sonner'

export default function ModelsPage() {
  // 从store获取状态
  const {
    searchQuery,
    filters,
    activeTab,
    showFilters,
    showCreateDialog,
    selectedModelId,
    editingModel,
    configModel,
    copiedModelKey,
    setSearchQuery,
    setFilters,
    setActiveTab,
    setShowFilters,
    setShowCreateDialog,
    setSelectedModelId,
    setEditingModel,
    setConfigModel,
    setDeleteModelId,
    setCopiedModelKey,
    setDeleteModelFunction
  } = useModelsStore()

  // 防抖搜索
  const debouncedSearchQuery = useDebouncedValue(searchQuery, 300)

  // 使用自定义hook获取数据
  const {
    models,
    publishedModels,
    loading,
    error,
    searchModels,
    refetch,
    publishModel,
    unpublishModel,
    deleteModel: deleteModelApi
  } = useModels()

  // 获取模型绑定状态
  const modelIds = models.map(m => m.id)
  const { bindings, refetch: refetchBindings } = useModelBindings(modelIds)

  // 处理搜索
  const handleSearch = useCallback(async (query: string) => {
    if (query.trim()) {
      await searchModels(query)
    } else {
      await refetch()
    }
  }, [searchModels, refetch])

  // 监听搜索输入变化
  useEffect(() => {
    if (debouncedSearchQuery !== undefined) {
      handleSearch(debouncedSearchQuery)
    }
  }, [debouncedSearchQuery, handleSearch])

  // 应用筛选条件
  const applyFilters = useCallback((newFilters: typeof filters) => {
    setFilters(newFilters)
  }, [setFilters])

  // 处理查看模型详情
  const handleViewDetails = useCallback((modelId: number) => {
    setSelectedModelId(modelId)
  }, [setSelectedModelId])

  // 处理发布/取消发布
  const handleTogglePublish = async (model: typeof models[0]) => {
    try {
      if (model.published) {
        await unpublishModel(model.id)
      } else {
        await publishModel(model.id)
      }
      refetch()
    } catch (error) {
      console.error('切换发布状态失败:', error)
      toast.error('操作失败，请重试')
    }
  }

  // 设置删除函数
  useEffect(() => {
    const deleteFn = async (id: number) => {
      await deleteModelApi(id)
      refetch()
    }
    setDeleteModelFunction(deleteFn)
  }, [deleteModelApi, refetch, setDeleteModelFunction])

  // 处理配置成功后的刷新
  const handleConfigSuccess = () => {
    refetch()
    refetchBindings()
  }

  // 处理复制modelKey
  const handleCopyModelKey = async (modelKey: string, e: React.MouseEvent) => {
    e.stopPropagation()
    try {
      await navigator.clipboard.writeText(modelKey)
      setCopiedModelKey(modelKey)
      setTimeout(() => setCopiedModelKey(null), 2000)
      toast.success(`已复制: ${modelKey}`)
    } catch (error) {
      console.error('复制失败:', error)
      toast.error('复制失败，请重试')
    }
  }

  // 处理刷新
  const handleRefresh = useCallback(async () => {
    await refetch()
    await refetchBindings()
  }, [refetch, refetchBindings])

  return (
    <>
      <PageHeader title="模型管理" />

      <div className="flex flex-1 flex-col gap-4 p-4 pt-0">
        {/* 页面标题和操作 */}
        <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
          <div>
            <h1 className="text-3xl font-bold tracking-tight">模型管理</h1>
            <p className="text-muted-foreground">
              管理和配置AI模型，包括基础模型和供应商模型
            </p>
          </div>
          <div className="flex items-center gap-2">
            <Button
              variant="outline"
              size="sm"
              onClick={() => setShowFilters(!showFilters)}
              className="gap-2"
            >
              筛选
            </Button>
            <Button
              onClick={() => setShowCreateDialog(true)}
              className="gap-2"
            >
              <Plus className="h-4 w-4" />
              新建模型
            </Button>
          </div>
        </div>

        {/* 搜索和筛选 */}
        <div className="space-y-4">
          <div className="relative">
            <Search className="absolute left-3 top-3 h-4 w-4 text-muted-foreground" />
            <Input
              placeholder="搜索模型名称..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="pl-9"
            />
          </div>

          {showFilters && (
            <ModelFilters
              filters={filters}
              onFiltersChange={applyFilters}
              onClose={() => setShowFilters(false)}
            />
          )}
        </div>

        {/* 模型列表 */}
        <ModelTabs
          models={models}
          publishedModels={publishedModels}
          loading={loading}
          error={error}
          activeTab={activeTab}
          filters={filters}
          bindings={bindings}
          copiedModelKey={copiedModelKey}
          onTabChange={(value) => setActiveTab(value as any)}
          onRefresh={handleRefresh}
          onViewDetails={handleViewDetails}
          onEdit={setEditingModel}
          onTogglePublish={handleTogglePublish}
          onConfigure={setConfigModel}
          onDelete={(model) => setDeleteModelId(model.id)}
          onCopyModelKey={handleCopyModelKey}
        />

        {/* 创建模型对话框 */}
        <ModelFormDialog
          open={showCreateDialog}
          onOpenChange={setShowCreateDialog}
          onSuccess={handleRefresh}
        />

        {/* 编辑模型对话框 */}
        <ModelFormDialog
          open={!!editingModel}
          onOpenChange={(open) => !open && setEditingModel(null)}
          model={editingModel}
          onSuccess={handleRefresh}
        />

        {/* 配置对话框 */}
        <ModelConfigDialog
          model={configModel}
          open={!!configModel}
          onOpenChange={(open) => !open && setConfigModel(null)}
          onSuccess={handleConfigSuccess}
        />

        {/* 模型详情对话框 */}
        <ModelDetailDialog
          open={!!selectedModelId}
          onOpenChange={(open) => !open && setSelectedModelId(null)}
          modelId={selectedModelId}
        />

        {/* 删除确认对话框 */}
        <ModelDeleteDialog />
      </div>
    </>
  )
}
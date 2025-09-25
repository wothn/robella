'use client'

import { useState, useCallback, useEffect } from 'react'
import { Plus, Search, Filter, BarChart3 } from 'lucide-react'
import { PageHeader } from '@/components/layout/page-header'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { Badge } from '@/components/ui/badge'
import { ModelList } from '@/components/models/model-list'
import { ModelFormDialog } from '@/components/models/model-form-dialog'
import { ModelDetailDialog } from '@/components/models/model-detail-dialog'
import { ModelFilters } from '@/components/models/model-filters'

import { useModels } from '@/hooks/use-models'
import { useDebouncedValue } from '@/hooks/useDebouncedValue'
import type { ModelFilters as ModelFiltersType } from '@/types/model'

export default function ModelsPage() {
  const [searchQuery, setSearchQuery] = useState('')
  const [filters, setFilters] = useState<ModelFiltersType>({})
  const [activeTab, setActiveTab] = useState<'all' | 'published' | 'draft'>('all')
  const [showCreateDialog, setShowCreateDialog] = useState(false)
  const [showFilters, setShowFilters] = useState(false)
  const [selectedModelId, setSelectedModelId] = useState<number | null>(null)

  // 防抖搜索
  const debouncedSearchQuery = useDebouncedValue(searchQuery, 300)

  // 使用自定义hook获取数据
  const {
    models,
    publishedModels,
    loading,
    error,
    searchModels,
    refetch
  } = useModels()

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
  const applyFilters = useCallback((newFilters: ModelFiltersType) => {
    setFilters(newFilters)
    // 这里可以根据筛选条件调用相应的API
  }, [])

  // 处理查看模型详情
  const handleViewDetails = useCallback((modelId: number) => {
    setSelectedModelId(modelId)
  }, [])

  // 获取当前标签页的数据
  const getCurrentTabData = () => {
    switch (activeTab) {
      case 'published':
        return publishedModels
      case 'draft':
        return models.filter((model: any) => !model.published)
      default:
        return models
    }
  }

  const currentData = getCurrentTabData()

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
              <Filter className="h-4 w-4" />
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
        <Tabs value={activeTab} onValueChange={(value: any) => setActiveTab(value)}>
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
            <ModelList
              models={currentData}
              loading={loading}
              error={error}
              onRefresh={refetch}
              onViewDetails={handleViewDetails}
            />
          </TabsContent>

          <TabsContent value="published" className="mt-6">
            <ModelList
              models={currentData}
              loading={loading}
              error={error}
              onRefresh={refetch}
              onViewDetails={handleViewDetails}
            />
          </TabsContent>

          <TabsContent value="draft" className="mt-6">
            <ModelList
              models={currentData}
              loading={loading}
              error={error}
              onRefresh={refetch}
              onViewDetails={handleViewDetails}
            />
          </TabsContent>
        </Tabs>

        {/* 创建模型对话框 */}
        <ModelFormDialog
          open={showCreateDialog}
          onOpenChange={setShowCreateDialog}
          onSuccess={refetch}
        />

        {/* 模型详情对话框 */}
        <ModelDetailDialog
          open={!!selectedModelId}
          onOpenChange={(open) => !open && setSelectedModelId(null)}
          modelId={selectedModelId}
        />
      </div>
    </>
  )
}

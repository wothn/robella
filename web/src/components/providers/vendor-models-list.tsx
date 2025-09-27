'use client'

import { useState, useMemo } from 'react'
import { VendorModel, UpdateVendorModelRequest } from '@/types'
import { ScrollArea } from '@/components/ui/scroll-area'
import { Bot } from 'lucide-react'
import { VendorModelFilters } from './vendor-model-filters'
import { VendorModelGrid } from './vendor-model-grid'
import { EmptyState } from '@/components/common/empty-state'
import { SearchBar } from '@/components/common/search-bar'
import { ModelSortOption, SortOrder } from '@/lib/constants'

interface VendorModelsListProps {
  vendorModels: VendorModel[]
  providerId: number
  onUpdateModel: (modelId: number, data: UpdateVendorModelRequest) => Promise<void>
  onDeleteModel: (modelId: number) => void
}

export function VendorModelsList({
  vendorModels,
  providerId,
  onUpdateModel,
  onDeleteModel
}: VendorModelsListProps) {
  const [searchQuery, setSearchQuery] = useState('')
  const [sortBy, setSortBy] = useState<ModelSortOption>('name')
  const [sortOrder, setSortOrder] = useState<SortOrder>('asc')

  // 过滤和排序逻辑
  const filteredAndSortedModels = useMemo(() => {
    const filtered = vendorModels.filter(model =>
      model.vendorModelName.toLowerCase().includes(searchQuery.toLowerCase()) ||
      model.vendorModelKey.toLowerCase().includes(searchQuery.toLowerCase()) ||
      (model.description && model.description.toLowerCase().includes(searchQuery.toLowerCase()))
    )

    // 排序
    filtered.sort((a, b) => {
      let aValue: string | number
      let bValue: string | number

      switch (sortBy) {
        case 'name':
          aValue = a.vendorModelName
          bValue = b.vendorModelName
          break
        case 'price':
          aValue = a.inputPerMillionTokens ? parseFloat(a.inputPerMillionTokens) : 0
          bValue = b.inputPerMillionTokens ? parseFloat(b.inputPerMillionTokens) : 0
          break
        case 'weight':
          aValue = a.weight ? a.weight : 0
          bValue = b.weight ? b.weight : 0
          break
        case 'status':
          aValue = a.enabled ? 1 : 0
          bValue = b.enabled ? 1 : 0
          break
        default:
          return 0
      }

      if (sortOrder === 'asc') {
        return aValue < bValue ? -1 : aValue > bValue ? 1 : 0
      } else {
        return aValue > bValue ? -1 : aValue < bValue ? 1 : 0
      }
    })

    return filtered
  }, [vendorModels, searchQuery, sortBy, sortOrder])

  // 空状态
  if (vendorModels.length === 0) {
    return (
      <EmptyState
        icon={<Bot className="h-12 w-12 text-muted-foreground" />}
        title="暂无模型"
        description="该 Provider 还没有添加任何模型"
      />
    )
  }

  // 搜索无结果状态
  if (filteredAndSortedModels.length === 0 && searchQuery) {
    return (
      <div className="space-y-4">
        <SearchBar
          placeholder="搜索模型名称、Key或描述..."
          value={searchQuery}
          onChange={setSearchQuery}
          onClear={() => setSearchQuery('')}
        />

        <EmptyState
          icon={<Bot className="h-12 w-12 text-muted-foreground" />}
          title="未找到匹配的模型"
          description="尝试调整搜索条件或清除搜索"
          action={{
            label: "清除搜索",
            onClick: () => setSearchQuery('')
          }}
        />
      </div>
    )
  }

  return (
    <div className="space-y-4">
      <VendorModelFilters
        searchQuery={searchQuery}
        onSearchChange={setSearchQuery}
        sortBy={sortBy}
        sortOrder={sortOrder}
        onSortByChange={setSortBy}
        onSortOrderChange={setSortOrder}
        totalModels={vendorModels.length}
        filteredModels={filteredAndSortedModels.length}
      />

      <ScrollArea className="h-[500px] pr-2">
        <div className="pr-2">
          <VendorModelGrid
            vendorModels={filteredAndSortedModels}
            providerId={providerId}
            onUpdateModel={onUpdateModel}
            onDeleteModel={onDeleteModel}
          />
        </div>
      </ScrollArea>
    </div>
  )
}
'use client'

import { SearchBar } from '@/components/common/search-bar'
import { SortFilter } from '@/components/common/sort-filter'
import { MODEL_SORT_OPTIONS, ModelSortOption, SortOrder } from '@/lib/constants'

interface VendorModelFiltersProps {
  searchQuery: string
  onSearchChange: (value: string) => void
  sortBy: ModelSortOption
  sortOrder: SortOrder
  onSortByChange: (value: ModelSortOption) => void
  onSortOrderChange: (order: SortOrder) => void
  totalModels: number
  filteredModels: number
}

export function VendorModelFilters({
  searchQuery,
  onSearchChange,
  sortBy,
  sortOrder,
  onSortByChange,
  onSortOrderChange,
  totalModels,
  filteredModels
}: VendorModelFiltersProps) {
  return (
    <div className="flex items-center gap-4 mb-4">
      <SearchBar
        placeholder="搜索模型名称、Key或描述..."
        value={searchQuery}
        onChange={onSearchChange}
        onClear={() => onSearchChange('')}
        className="flex-1"
      />

      <SortFilter
        sortBy={sortBy}
        sortOrder={sortOrder}
        sortOptions={[...MODEL_SORT_OPTIONS]}
        onSortByChange={onSortByChange}
        onSortOrderChange={onSortOrderChange}
      />

      <div className="text-sm text-muted-foreground whitespace-nowrap">
        {filteredModels} / {totalModels} 个模型
      </div>
    </div>
  )
}
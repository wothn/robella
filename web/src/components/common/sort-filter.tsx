'use client'

import { Button } from '@/components/ui/button'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { ArrowUpDown, Filter } from 'lucide-react'
import { cn } from '@/lib/utils'

interface SortFilterProps<T extends string> {
  sortBy: T
  sortOrder: 'asc' | 'desc'
  sortOptions: Array<{ value: T; label: string }>
  onSortByChange: (value: T) => void
  onSortOrderChange: (order: 'asc' | 'desc') => void
  className?: string
}

export function SortFilter<T extends string>({
  sortBy,
  sortOrder,
  sortOptions,
  onSortByChange,
  onSortOrderChange,
  className
}: SortFilterProps<T>) {
  return (
    <div className={cn("flex items-center gap-2", className)}>
      <Filter className="h-4 w-4 text-muted-foreground" />
      <Select value={sortBy} onValueChange={onSortByChange}>
        <SelectTrigger className="w-auto">
          <SelectValue />
        </SelectTrigger>
        <SelectContent>
          {sortOptions.map((option) => (
            <SelectItem key={option.value} value={option.value}>
              {option.label}
            </SelectItem>
          ))}
        </SelectContent>
      </Select>

      <Button
        variant="ghost"
        size="sm"
        onClick={() => onSortOrderChange(sortOrder === 'asc' ? 'desc' : 'asc')}
        className="px-2"
      >
        <ArrowUpDown className={`h-4 w-4 ${sortOrder === 'desc' ? 'rotate-180' : ''}`} />
      </Button>
    </div>
  )
}
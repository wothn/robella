'use client'

import { Search } from 'lucide-react'
import { Input } from '@/components/ui/input'
import { Badge } from '@/components/ui/badge'
import { Separator } from '@/components/ui/separator'

interface SearchSectionProps {
  searchQuery: string
  onSearchChange: (value: string) => void
  boundCount: number
}

export function SearchSection({ searchQuery, onSearchChange, boundCount }: SearchSectionProps) {
  return (
    <div className="space-y-4">
      <div className="relative">
        <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-muted-foreground" />
        <Input
          placeholder="搜索供应商或模型..."
          value={searchQuery}
          onChange={(e) => onSearchChange(e.target.value)}
          className="pl-10"
        />
      </div>

      <div className="flex items-center gap-2">
        <span className="text-sm text-muted-foreground">
          已绑定 {boundCount} 个供应商模型
        </span>
        {boundCount > 0 && (
          <Badge variant="secondary">
            {boundCount} 个绑定
          </Badge>
        )}
      </div>

      <Separator />
    </div>
  )
}
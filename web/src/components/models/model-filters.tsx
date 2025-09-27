import { useState, useEffect } from 'react'
import { X, Filter } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Switch } from '@/components/ui/switch'
import { Badge } from '@/components/ui/badge'
import { CAPABILITY_OPTIONS, ORGANIZATION_OPTIONS } from '@/constants/model-constants'
import type { ModelFilters as ModelFiltersType } from '@/types/model'

interface ModelFiltersProps {
  filters: ModelFiltersType
  onFiltersChange: (filters: ModelFiltersType) => void
  onClose: () => void
}

export function ModelFilters({ filters, onFiltersChange, onClose }: ModelFiltersProps) {
  const [localFilters, setLocalFilters] = useState<ModelFiltersType>(filters)

  useEffect(() => {
    setLocalFilters(filters)
  }, [filters])

  const handleFilterChange = (key: keyof ModelFiltersType, value: string | boolean | undefined) => {
    const newFilters = { ...localFilters, [key]: value }
    setLocalFilters(newFilters)
  }

  const handleApplyFilters = () => {
    onFiltersChange(localFilters)
    onClose()
  }

  const handleResetFilters = () => {
    const resetFilters: ModelFiltersType = {}
    setLocalFilters(resetFilters)
    onFiltersChange(resetFilters)
  }

  const getActiveFilterCount = () => {
    return Object.values(localFilters).filter(value => 
      value !== undefined && value !== '' && value !== null
    ).length
  }

  return (
    <Card className="w-full">
      <CardHeader className="pb-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Filter className="h-4 w-4" />
            <CardTitle className="text-lg">筛选条件</CardTitle>
            {getActiveFilterCount() > 0 && (
              <Badge variant="secondary" className="text-xs">
                {getActiveFilterCount()} 个筛选
              </Badge>
            )}
          </div>
          <Button variant="ghost" size="sm" onClick={onClose}>
            <X className="h-4 w-4" />
          </Button>
        </div>
        <CardDescription>
          根据条件筛选模型列表
        </CardDescription>
      </CardHeader>
      
      <CardContent className="space-y-6">
        {/* 搜索关键词 */}
        <div className="space-y-2">
          <Label htmlFor="query">关键词搜索</Label>
          <Input
            id="query"
            placeholder="搜索模型名称或描述..."
            value={localFilters.query || ''}
            onChange={(e) => handleFilterChange('query', e.target.value)}
          />
        </div>

        {/* 组织筛选 */}
        <div className="space-y-2">
          <Label htmlFor="organization">组织</Label>
          <Select
            value={localFilters.organization || ''}
            onValueChange={(value) => handleFilterChange('organization', value || undefined)}
          >
            <SelectTrigger>
              <SelectValue placeholder="选择组织" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="">全部组织</SelectItem>
              {ORGANIZATION_OPTIONS.map((org) => (
                <SelectItem key={org} value={org}>
                  {org}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>

        {/* 能力筛选 */}
        <div className="space-y-2">
          <Label htmlFor="capability">模型能力</Label>
          <Select
            value={localFilters.capability || ''}
            onValueChange={(value) => handleFilterChange('capability', value || undefined)}
          >
            <SelectTrigger>
              <SelectValue placeholder="选择能力类型" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="">全部能力</SelectItem>
              {CAPABILITY_OPTIONS.map((capability) => (
                <SelectItem key={capability.value} value={capability.value}>
                  {capability.label}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>

        {/* 发布状态 */}
        <div className="space-y-3">
          <Label>发布状态</Label>
          <div className="flex items-center space-x-2">
            <Switch
              id="published"
              checked={localFilters.published === true}
              onCheckedChange={(checked) => 
                handleFilterChange('published', checked ? true : undefined)
              }
            />
            <Label htmlFor="published" className="text-sm">
              仅显示已发布模型
            </Label>
          </div>
        </div>

        {/* 操作按钮 */}
        <div className="flex gap-2 pt-4">
          <Button onClick={handleApplyFilters} className="flex-1">
            应用筛选
          </Button>
          <Button variant="outline" onClick={handleResetFilters}>
            重置
          </Button>
        </div>

        {/* 当前筛选条件显示 */}
        {getActiveFilterCount() > 0 && (
          <div className="space-y-2 pt-4 border-t">
            <Label className="text-sm font-medium">当前筛选</Label>
            <div className="flex flex-wrap gap-2">
              {localFilters.query && (
                <Badge variant="outline" className="gap-1">
                  关键词: {localFilters.query}
                  <X 
                    className="h-3 w-3 cursor-pointer" 
                    onClick={() => handleFilterChange('query', undefined)}
                  />
                </Badge>
              )}
              {localFilters.organization && (
                <Badge variant="outline" className="gap-1">
                  组织: {localFilters.organization}
                  <X 
                    className="h-3 w-3 cursor-pointer" 
                    onClick={() => handleFilterChange('organization', undefined)}
                  />
                </Badge>
              )}
              {localFilters.capability && (
                <Badge variant="outline" className="gap-1">
                  能力: {CAPABILITY_OPTIONS.find(c => c.value === localFilters.capability)?.label}
                  <X
                    className="h-3 w-3 cursor-pointer"
                    onClick={() => handleFilterChange('capability', undefined)}
                  />
                </Badge>
              )}
              {localFilters.published && (
                <Badge variant="outline" className="gap-1">
                  已发布
                  <X 
                    className="h-3 w-3 cursor-pointer" 
                    onClick={() => handleFilterChange('published', undefined)}
                  />
                </Badge>
              )}
            </div>
          </div>
        )}
      </CardContent>
    </Card>
  )
}

'use client'

import { useState, useMemo } from 'react'
import { VendorModel, UpdateVendorModelRequest } from '@/types'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Input } from '@/components/ui/input'
import { 
  Bot, 
  Trash2, 
  Settings, 
  DollarSign, 
  Clock, 
  Target, 
  ToggleLeft, 
  ToggleRight,
  Search,
  ArrowUpDown,
  Filter
} from 'lucide-react'
import { VendorModelModal } from '@/components/providers/vendor-model-modal'
import { cn } from '@/lib/utils'
import { formatCurrency } from '@/lib/formatters'

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
  const [sortBy, setSortBy] = useState<'name' | 'price' | 'weight' | 'status'>('name')
  const [sortOrder, setSortOrder] = useState<'asc' | 'desc'>('asc')

  // 过滤和排序逻辑
  const filteredAndSortedModels = useMemo(() => {
    let filtered = vendorModels.filter(model =>
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
          aValue = parseFloat(a.inputPerMillionTokens || '0')
          bValue = parseFloat(b.inputPerMillionTokens || '0')
          break
        case 'weight':
          aValue = parseFloat(a.weight || '0')
          bValue = parseFloat(b.weight || '0')
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

  if (vendorModels.length === 0) {
    return (
      <Card>
        <CardContent className="flex flex-col items-center justify-center py-12">
          <Bot className="h-12 w-12 text-muted-foreground mb-4" />
          <h3 className="text-lg font-medium text-foreground mb-2">暂无模型</h3>
          <p className="text-muted-foreground text-center">
            该 Provider 还没有添加任何模型
          </p>
        </CardContent>
      </Card>
    )
  }

  if (filteredAndSortedModels.length === 0 && searchQuery) {
    return (
      <div className="space-y-4">
        {/* 搜索栏 */}
        <div className="flex items-center gap-4">
          <div className="relative flex-1">
            <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-muted-foreground h-4 w-4" />
            <Input
              placeholder="搜索模型名称、Key或描述..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="pl-10"
            />
          </div>
        </div>
        
        <Card>
          <CardContent className="flex flex-col items-center justify-center py-12">
            <Search className="h-12 w-12 text-muted-foreground mb-4" />
            <h3 className="text-lg font-medium text-foreground mb-2">未找到匹配的模型</h3>
            <p className="text-muted-foreground text-center">
              尝试调整搜索条件或清除搜索
            </p>
            <Button 
              variant="outline" 
              onClick={() => setSearchQuery('')}
              className="mt-4"
            >
              清除搜索
            </Button>
          </CardContent>
        </Card>
      </div>
    )
  }

  const getProviderTypeLabel = (providerType: string) => {
    const labels: Record<string, string> = {
      'NONE': '无转换器',
      'OPENAI': 'OpenAI',
      'VOLCANOENGINE': '火山引擎',
      'ZHIPU': '智谱AI',
      'DASHSCOPE': '通义千问'
    }
    return labels[providerType] || providerType
  }

  const formatPrice = (price: string | undefined, currency: string = 'USD') => {
    if (!price) return null
    const numPrice = parseFloat(price)
    if (isNaN(numPrice)) return null
    return `${formatCurrency(numPrice)} ${currency}/1M`
  }

  const formatWeight = (weight: string | undefined) => {
    if (!weight) return null
    const numWeight = parseFloat(weight)
    if (isNaN(numWeight)) return null
    return numWeight.toFixed(1)
  }

  return (
    <>
      {/* 搜索和排序栏 */}
      <div className="flex items-center gap-4 mb-4">
        <div className="relative flex-1">
          <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-muted-foreground h-4 w-4" />
          <Input
            placeholder="搜索模型名称、Key或描述..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="pl-10"
          />
        </div>
        
        <div className="flex items-center gap-2">
          <Filter className="h-4 w-4 text-muted-foreground" />
          <select
            value={sortBy}
            onChange={(e) => setSortBy(e.target.value as any)}
            className="text-sm border rounded-md px-2 py-1 bg-background"
          >
            <option value="name">名称</option>
            <option value="price">价格</option>
            <option value="weight">权重</option>
            <option value="status">状态</option>
          </select>
          
          <Button
            variant="ghost"
            size="sm"
            onClick={() => setSortOrder(sortOrder === 'asc' ? 'desc' : 'asc')}
            className="px-2"
          >
            <ArrowUpDown className={`h-4 w-4 ${sortOrder === 'desc' ? 'rotate-180' : ''}`} />
          </Button>
        </div>
        
        <div className="text-sm text-muted-foreground">
          {filteredAndSortedModels.length} / {vendorModels.length} 个模型
        </div>
      </div>

      <div className="grid gap-4">
        {filteredAndSortedModels.map((model) => (
          <Card key={model.id} className={cn(
            "transition-all hover:shadow-md",
            !model.enabled && "opacity-75"
          )}>
            <CardHeader className="pb-3">
              <div className="flex items-start justify-between">
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2 mb-2">
                    <CardTitle className="text-lg font-semibold truncate">
                      {model.vendorModelName}
                    </CardTitle>
                    <Badge variant="outline" className="text-xs font-mono">
                      {model.vendorModelKey}
                    </Badge>
                    <Badge 
                      variant={model.enabled ? 'default' : 'secondary'}
                      className="flex items-center gap-1"
                    >
                      {model.enabled ? (
                        <ToggleRight className="h-3 w-3" />
                      ) : (
                        <ToggleLeft className="h-3 w-3" />
                      )}
                      {model.enabled ? '启用' : '禁用'}
                    </Badge>
                  </div>
                  
                  {model.description && (
                    <p className="text-sm text-muted-foreground line-clamp-2">
                      {model.description}
                    </p>
                  )}
                </div>
                
                <div className="flex items-center gap-2 ml-4">
                  <VendorModelModal
                    vendorModel={model}
                    providerId={providerId}
                    onSubmit={(data) => onUpdateModel(model.id, data)}
                  />
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => onDeleteModel(model.id)}
                    className="text-destructive hover:text-destructive hover:bg-destructive/10"
                  >
                    <Trash2 className="h-4 w-4" />
                  </Button>
                </div>
              </div>
            </CardHeader>
            
            <CardContent className="pt-0">
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
                {/* 提供商类型 */}
                <div className="flex items-center gap-2">
                  <Settings className="h-4 w-4 text-muted-foreground" />
                  <div>
                    <p className="text-xs text-muted-foreground">转换器</p>
                    <p className="text-sm font-medium">{getProviderTypeLabel(model.providerType)}</p>
                  </div>
                </div>

                {/* 权重 */}
                {model.weight && (
                  <div className="flex items-center gap-2">
                    <Target className="h-4 w-4 text-muted-foreground" />
                    <div>
                      <p className="text-xs text-muted-foreground">权重</p>
                      <p className="text-sm font-medium">{formatWeight(model.weight)}</p>
                    </div>
                  </div>
                )}

                {/* 输入价格 */}
                {model.inputPerMillionTokens && (
                  <div className="flex items-center gap-2">
                    <DollarSign className="h-4 w-4 text-muted-foreground" />
                    <div>
                      <p className="text-xs text-muted-foreground">输入价格</p>
                      <p className="text-sm font-medium">
                        {formatPrice(model.inputPerMillionTokens, model.currency)}
                      </p>
                    </div>
                  </div>
                )}

                {/* 输出价格 */}
                {model.outputPerMillionTokens && (
                  <div className="flex items-center gap-2">
                    <DollarSign className="h-4 w-4 text-muted-foreground" />
                    <div>
                      <p className="text-xs text-muted-foreground">输出价格</p>
                      <p className="text-sm font-medium">
                        {formatPrice(model.outputPerMillionTokens, model.currency)}
                      </p>
                    </div>
                  </div>
                )}

                {/* 缓存输入价格 */}
                {model.cachedInputPrice && (
                  <div className="flex items-center gap-2">
                    <Clock className="h-4 w-4 text-muted-foreground" />
                    <div>
                      <p className="text-xs text-muted-foreground">缓存输入</p>
                      <p className="text-sm font-medium">
                        {formatPrice(model.cachedInputPrice, model.currency)}
                      </p>
                    </div>
                  </div>
                )}


              </div>
            </CardContent>
          </Card>
        ))}
      </div>
    </>
  )
}
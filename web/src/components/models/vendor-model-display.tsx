'use client'

import { VendorModel } from '@/types'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import {
  Settings,
  DollarSign,
  Clock,
  Target,
  ToggleLeft,
  ToggleRight,
  ChevronDown
} from 'lucide-react'
import { useState } from 'react'
import { cn } from '@/lib/utils'
import { formatPriceDisplay, formatWeightDisplay, getProviderTypeLabel } from '@/lib/formatters'

interface VendorModelDisplayProps {
  vendorModels: VendorModel[]
  providers: Array<{
    id: number
    name: string
  }>
}

export function VendorModelDisplay({ vendorModels, providers }: VendorModelDisplayProps) {
  const [expandedProviders, setExpandedProviders] = useState<Record<number, boolean>>({})

  // 根据providerId获取provider名称
  const getProviderName = (providerId: number): string => {
    const provider = providers.find(p => p.id === providerId)
    return provider?.name || `Provider ${providerId}`
  }

  // 切换供应商展开状态
  const toggleProvider = (providerId: number) => {
    setExpandedProviders(prev => ({
      ...prev,
      [providerId]: !prev[providerId]
    }))
  }

  if (vendorModels.length === 0) {
    return (
      <Card>
        <CardContent className="flex flex-col items-center justify-center py-12">
          <Settings className="h-12 w-12 text-muted-foreground mb-4" />
          <p className="text-muted-foreground text-center">
            暂无关联的供应商模型
          </p>
          <p className="text-sm text-muted-foreground text-center mt-1">
            您可以在供应商管理页面配置模型关联
          </p>
        </CardContent>
      </Card>
    )
  }

  // 按供应商分组
  const groupedModels = vendorModels.reduce((acc, model) => {
    if (!acc[model.providerId]) {
      acc[model.providerId] = []
    }
    acc[model.providerId].push(model)
    return acc
  }, {} as Record<number, VendorModel[]>)

  return (
    <div className="space-y-6">
      {Object.entries(groupedModels).map(([providerIdStr, models]) => {
        const providerId = parseInt(providerIdStr)
        const providerName = getProviderName(providerId)
        const isExpanded = expandedProviders[providerId] ?? true

        return (
          <div key={providerId} className="space-y-4">
            {/* 供应商标题 */}
            <div 
              className="flex items-center justify-between cursor-pointer"
              onClick={() => toggleProvider(providerId)}
            >
              <div className="flex items-center gap-2">
                <ChevronDown 
                  className={`h-4 w-4 transition-transform ${isExpanded ? 'rotate-0' : '-rotate-90'}`} 
                />
                <h3 className="text-lg font-semibold">{providerName}</h3>
              </div>
              <Badge variant="outline">
                {models.length} 个模型
              </Badge>
            </div>

            {/* 模型列表 */}
            {isExpanded && (
              <div className="grid gap-4 pl-6">
                {models.map((model) => (
                  <Card
                    key={model.id}
                    className={cn(
                      "transition-all",
                      !model.enabled && "opacity-75"
                    )}
                  >
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
                              <p className="text-sm font-medium">{formatWeightDisplay(model.weight)}</p>
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
                                {formatPriceDisplay(model.inputPerMillionTokens, model.currency)}
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
                                {formatPriceDisplay(model.outputPerMillionTokens, model.currency)}
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
                                {formatPriceDisplay(model.cachedInputPrice, model.currency)}
                              </p>
                            </div>
                          </div>
                        )}
                      </div>
                    </CardContent>
                  </Card>
                ))}
              </div>
            )}
          </div>
        )
      })}
    </div>
  )
}
'use client'

import { VendorModel, UpdateVendorModelRequest } from '@/types'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import {
  Settings,
  DollarSign,
  Clock,
  Target,
  ToggleLeft,
  ToggleRight,
  Trash2
} from 'lucide-react'
import { VendorModelModal } from './vendor-model-modal'
import { cn } from '@/lib/utils'
import { formatPriceDisplay, formatWeightDisplay, getProviderTypeLabel } from '@/lib/formatters'

interface VendorModelGridProps {
  vendorModels: VendorModel[]
  providerId: number
  onUpdateModel: (modelId: number, data: UpdateVendorModelRequest) => Promise<void>
  onDeleteModel: (modelId: number) => void
}

export function VendorModelGrid({
  vendorModels,
  providerId,
  onUpdateModel,
  onDeleteModel
}: VendorModelGridProps) {
  return (
    <div className="grid gap-4">
      {vendorModels.map((model) => (
        <Card
          key={model.id}
          className={cn(
            "transition-all hover:shadow-md",
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

              <div className="flex items-center gap-2 ml-4">
                <VendorModelModal
                  vendorModel={model}
                  providerId={providerId}
                  onSubmit={(data) => onUpdateModel(model.id, data)}
                />
                <Button
                  variant="ghost"
                  size="sm"
                  className="text-destructive hover:text-destructive hover:bg-destructive/10"
                  onClick={() => onDeleteModel(model.id)}
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
  )
}
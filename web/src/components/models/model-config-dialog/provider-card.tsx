'use client'

import { ChevronRight, ChevronDown } from 'lucide-react'
import { Card, CardContent } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import type { Provider } from '@/types/provider'
import type { VendorModel } from '@/types/vendor-model'
import { VendorModelItem } from './vendor-model-item'

interface ProviderWithModels extends Provider {
  vendorModels: VendorModel[]
  expanded: boolean
}

interface ProviderCardProps {
  provider: ProviderWithModels
  isModelBound: (vendorModelId: number) => boolean
  onToggle: (providerId: number) => void
  onToggleModel: (vendorModel: VendorModel) => void
}

export function ProviderCard({ provider, isModelBound, onToggle, onToggleModel }: ProviderCardProps) {
  return (
    <Card className="overflow-hidden">
      <CardContent className="p-0">
        <div
          className="flex items-center justify-between p-4 cursor-pointer hover:bg-muted/50 transition-colors"
          onClick={() => onToggle(provider.id)}
        >
          <div className="flex items-center gap-3">
            {provider.expanded ? (
              <ChevronDown className="h-4 w-4 text-muted-foreground" />
            ) : (
              <ChevronRight className="h-4 w-4 text-muted-foreground" />
            )}
            <div>
              <h3 className="font-medium">{provider.name}</h3>
              <p className="text-sm text-muted-foreground">
                {provider.endpointType} • {provider.vendorModels.length} 个模型
              </p>
            </div>
          </div>
          <Badge variant={provider.enabled ? "default" : "secondary"}>
            {provider.enabled ? '启用' : '禁用'}
          </Badge>
        </div>

        {provider.expanded && provider.vendorModels.length > 0 && (
          <div className="px-4 pb-4 space-y-2">
            {provider.vendorModels.map((vendorModel) => (
              <VendorModelItem
                key={vendorModel.id}
                vendorModel={vendorModel}
                isBound={isModelBound(vendorModel.id)}
                onToggle={onToggleModel}
              />
            ))}
          </div>
        )}

        {provider.expanded && provider.vendorModels.length === 0 && (
          <div className="px-4 pb-4">
            <div className="text-center py-4 text-muted-foreground">
              该供应商暂无可用模型
            </div>
          </div>
        )}
      </CardContent>
    </Card>
  )
}
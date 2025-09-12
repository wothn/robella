'use client'

import { Provider } from '@/types'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Plus, Settings } from 'lucide-react'

interface ProviderListProps {
  providers: Provider[]
  selectedProvider: Provider | null
  onProviderSelect: (provider: Provider) => void
  onCreateNew: () => void
}

export function ProviderList({
  providers,
  selectedProvider,
  onProviderSelect,
  onCreateNew
}: ProviderListProps) {
  return (
    <div className="w-80 border-r bg-gray-50/50 overflow-y-auto">
      <div className="p-4 border-b">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-lg font-semibold">Providers</h2>
          <Button size="sm" onClick={onCreateNew}>
            <Plus className="h-4 w-4 mr-1" />
            新建
          </Button>
        </div>
      </div>
      
      <div className="p-2 space-y-1">
        {providers.length === 0 ? (
          <div className="text-center py-8 text-gray-500">
            <Settings className="h-8 w-8 mx-auto mb-2 opacity-50" />
            <p>暂无 Providers</p>
            <p className="text-sm">点击上方"新建"按钮添加</p>
          </div>
        ) : (
          providers.map((provider) => (
            <div
              key={provider.id}
              className={`p-3 rounded-lg cursor-pointer transition-colors ${
                selectedProvider?.id === provider.id
                  ? 'bg-blue-100 border-blue-200 border'
                  : 'hover:bg-gray-100'
              }`}
              onClick={() => onProviderSelect(provider)}
            >
              <div className="flex items-center justify-between">
                <div className="flex-1 min-w-0">
                  <h3 className="font-medium truncate">{provider.name}</h3>
                  <p className="text-sm text-gray-500 truncate">{provider.type}</p>
                </div>
                <Badge variant={provider.enabled ? 'default' : 'secondary'}>
                  {provider.enabled ? '启用' : '禁用'}
                </Badge>
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  )
}
'use client'

import { Provider } from '@/types'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Plus, Search } from 'lucide-react'
import { ScrollArea } from '@/components/ui/scroll-area'
import { useState, useMemo } from 'react'
import { SearchBar } from '@/components/common/search-bar'
import { EmptyState } from '@/components/common/empty-state'

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
  const [searchTerm, setSearchTerm] = useState('')

  const filteredProviders = useMemo(() => {
    if (!searchTerm.trim()) {
      return providers
    }
    
    const term = searchTerm.toLowerCase()
    return providers.filter(provider => {
      const name = provider.name || ''
      const providerType = provider.providerType || ''
      const endpointType = provider.endpointType || ''
      
      return (
        name.toLowerCase().includes(term) ||
        providerType.toLowerCase().includes(term) ||
        endpointType.toLowerCase().includes(term)
      )
    })
  }, [providers, searchTerm])

  return (
    <div className="w-80 border-r bg-card overflow-hidden flex flex-col">
      <div className="p-4 border-b bg-card">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-lg font-semibold">Providers</h2>
          <Button size="sm" onClick={onCreateNew}>
            <Plus className="h-4 w-4 mr-1" />
            新建
          </Button>
        </div>
        
        {/* 搜索框 */}
        <SearchBar
          placeholder="搜索 Providers..."
          value={searchTerm}
          onChange={setSearchTerm}
          onClear={() => setSearchTerm('')}
        />
      </div>
      
      <ScrollArea className="flex-1">
        <div className="p-2 pr-4 space-y-1">
          {filteredProviders.length === 0 ? (
            <EmptyState
              icon={
                <div className="mx-auto h-12 w-12 rounded-full bg-muted flex items-center justify-center">
                  <Search className="h-6 w-6 opacity-50" />
                </div>
              }
              title="未找到匹配的 Providers"
              description="尝试其他搜索关键词"
            />
          ) : (
            filteredProviders.map((provider) => (
              <div
                key={provider.id}
                className={`relative p-3 rounded-lg cursor-pointer transition-all duration-200 border ${
                  selectedProvider?.id === provider.id
                    ? 'bg-primary/5 border-primary shadow-sm'
                    : 'border-transparent hover:bg-muted hover:border-border'
                }`}
                onClick={() => onProviderSelect(provider)}
              >
                <div className="flex items-center justify-between">
                  <div className="flex-1 min-w-0 space-y-1">
                    <h3 className="font-medium text-sm truncate text-foreground">{provider.name}</h3>
                    <p className="text-xs text-muted-foreground truncate">
                      {provider.endpointType} / {provider.providerType}
                    </p>
                  </div>
                  <Badge variant={provider.enabled ? 'default' : 'secondary'} className="ml-2 flex-shrink-0">
                    {provider.enabled ? '启用' : '禁用'}
                  </Badge>
                </div>
                {selectedProvider?.id === provider.id && (
                  <div className="absolute inset-y-0 right-0 w-1 bg-primary rounded-l-full"></div>
                )}
              </div>
            ))
          )}
        </div>
      </ScrollArea>
    </div>
  )
}
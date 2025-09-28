'use client'

import { useState, useEffect, useMemo, useCallback } from 'react'
import { Link } from 'lucide-react'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { ScrollArea } from '@/components/ui/scroll-area'
import { useToast } from '@/hooks/use-toast'
import { useProviders } from '@/hooks/use-providers'
import { api } from '@/lib/api'
import type { Model } from '@/types/model'
import type { Provider } from '@/types/provider'
import type { VendorModel } from '@/types/vendor-model'
import { SearchSection } from './model-config-dialog/search-section'
import { ProviderCard } from './model-config-dialog/provider-card'
import { LoadingState, EmptyState } from './model-config-dialog/loading-states'

interface ModelConfigDialogProps {
  model: Model | null
  open: boolean
  onOpenChange: (open: boolean) => void
  onSuccess?: () => void
}

interface ProviderWithModels extends Provider {
  vendorModels: VendorModel[]
  expanded: boolean
}

export function ModelConfigDialog({ 
  model, 
  open, 
  onOpenChange, 
  onSuccess 
}: ModelConfigDialogProps) {
  const [searchQuery, setSearchQuery] = useState('')
  const [providers, setProviders] = useState<ProviderWithModels[]>([])
  const [boundVendorModels, setBoundVendorModels] = useState<VendorModel[]>([])
  const [loading, setLoading] = useState(false)
  const { toast } = useToast()
  const { providers: allProviders } = useProviders()

  // 加载数据
  useEffect(() => {
    if (open && model) {
      loadData()
    }
  }, [open, model, allProviders])

  // 关闭对话框时重置状态
  useEffect(() => {
    if (!open) {
      setSearchQuery('')
      setProviders([])
      setBoundVendorModels([])
    }
  }, [open])

  const loadData = async () => {
    if (!model) return
    
    setLoading(true)
    try {
      // 获取已绑定的VendorModel
      const bound = await api.getVendorModelsByModelId(model.id)
      setBoundVendorModels(bound)

      // 获取所有供应商及其VendorModel
      const providersWithModels = await Promise.all(
        allProviders.map(async (provider) => {
          try {
            const vendorModels = await api.getVendorModelsByProviderId(provider.id)
            return {
              ...provider,
              vendorModels,
              expanded: false
            }
          } catch (error) {
            console.error(`Failed to load vendor models for provider ${provider.id}:`, error)
            return {
              ...provider,
              vendorModels: [],
              expanded: false
            }
          }
        })
      )
      
      setProviders(providersWithModels)
    } catch (error) {
      console.error('Failed to load configuration data:', error)
      toast({
        title: '加载失败',
        description: '无法加载配置数据，请重试',
        variant: 'destructive',
      })
    } finally {
      setLoading(false)
    }
  }

  // 切换供应商展开状态 - 使用useCallback优化性能
  const toggleProvider = useCallback((providerId: number) => {
    setProviders(prev => prev.map(p => 
      p.id === providerId ? { ...p, expanded: !p.expanded } : p
    ))
  }, [])

  // 检查VendorModel是否已绑定 - 使用useCallback优化性能
  const isVendorModelBound = useCallback((vendorModelId: number) => {
    return boundVendorModels.some(vm => vm.id === vendorModelId)
  }, [boundVendorModels])

  // 绑定/解绑VendorModel
  const toggleVendorModel = async (vendorModel: VendorModel) => {
    if (!model) return

    const isBound = isVendorModelBound(vendorModel.id)
    
    try {
      if (isBound) {
        // 解绑：更新VendorModel，移除modelId
        await api.updateVendorModel(vendorModel.id, { modelId: undefined })
        setBoundVendorModels(prev => prev.filter(vm => vm.id !== vendorModel.id))
        toast({
          title: '解绑成功',
          description: `已从模型 "${model.name}" 解绑 "${vendorModel.vendorModelName}"`,
        })
      } else {
        // 绑定：调用绑定API
        const updatedVendorModel = await api.addVendorModelToModel(model.id, vendorModel.id)
        setBoundVendorModels(prev => [...prev, updatedVendorModel])
        toast({
          title: '绑定成功',
          description: `已将 "${vendorModel.vendorModelName}" 绑定到模型 "${model.name}"`,
        })
      }
      onSuccess?.()
    } catch (error) {
      console.error('Toggle vendor model failed:', error)
      toast({
        title: isBound ? '解绑失败' : '绑定失败',
        description: '操作失败，请重试',
        variant: 'destructive',
      })
    }
  }

  // 过滤供应商和VendorModel - 使用useMemo优化性能
  const filteredProviders = useMemo(() => {
    return providers.map(provider => {
      const filteredVendorModels = provider.vendorModels.filter(vm =>
        vm.vendorModelName.toLowerCase().includes(searchQuery.toLowerCase()) ||
        vm.vendorModelKey.toLowerCase().includes(searchQuery.toLowerCase()) ||
        (vm.description && vm.description.toLowerCase().includes(searchQuery.toLowerCase()))
      )

      const shouldShowProvider = searchQuery === '' || 
        provider.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
        filteredVendorModels.length > 0

      return shouldShowProvider ? {
        ...provider,
        vendorModels: filteredVendorModels
      } : null
    }).filter(Boolean) as ProviderWithModels[]
  }, [providers, searchQuery])

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-4xl">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <Link className="h-5 w-5" />
            配置模型: {model?.name}
          </DialogTitle>
          <DialogDescription>
            选择要绑定到此模型的供应商模型。已绑定的模型将用于处理请求。
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-4 flex flex-col">
          <SearchSection
            searchQuery={searchQuery}
            onSearchChange={setSearchQuery}
            boundCount={boundVendorModels.length}
          />

          <div className="flex-1 min-h-0">
            <ScrollArea className="h-[60vh]">
              {loading ? (
                <LoadingState />
              ) : filteredProviders.length === 0 ? (
                <EmptyState hasSearchQuery={!!searchQuery} />
              ) : (
                <div className="space-y-2">
                  {filteredProviders.map((provider) => (
                    <ProviderCard
                      key={provider.id}
                      provider={provider}
                      isModelBound={isVendorModelBound}
                      onToggle={toggleProvider}
                      onToggleModel={toggleVendorModel}
                    />
                  ))}
                </div>
              )}
            </ScrollArea>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  )
}
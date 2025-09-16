'use client'

import { useState, useEffect, useMemo, useCallback } from 'react'
import { Search, ChevronRight, ChevronDown, Link, Unlink } from 'lucide-react'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Badge } from '@/components/ui/badge'
import { Card, CardContent } from '@/components/ui/card'
import { ScrollArea } from '@/components/ui/scroll-area'
import { Separator } from '@/components/ui/separator'
import { useToast } from '@/hooks/use-toast'
import { useProviders } from '@/hooks/use-providers'
import { api } from '@/lib/api'
import type { Model } from '@/types/model'
import type { Provider } from '@/types/provider'
import type { VendorModel } from '@/types/vendor-model'

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
        vm.modelKey.toLowerCase().includes(searchQuery.toLowerCase()) ||
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
      <DialogContent className="max-w-4xl max-h-[80vh] overflow-hidden flex flex-col">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <Link className="h-5 w-5" />
            配置模型: {model?.name}
          </DialogTitle>
          <DialogDescription>
            选择要绑定到此模型的供应商模型。已绑定的模型将用于处理请求。
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-4">
          {/* 搜索框 */}
          <div className="relative">
            <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 h-4 w-4" />
            <Input
              placeholder="搜索供应商或模型..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="pl-10"
            />
          </div>

          {/* 已绑定模型统计 */}
          <div className="flex items-center justify-between">
            <span className="text-sm text-muted-foreground">
              已绑定 {boundVendorModels.length} 个供应商模型
            </span>
            {boundVendorModels.length > 0 && (
              <Badge variant="secondary">
                {boundVendorModels.length} 个绑定
              </Badge>
            )}
          </div>

          <Separator />

          {/* 供应商和模型列表 */}
          <ScrollArea className="h-[400px] w-full">
            {loading ? (
              <div className="flex items-center justify-center h-32">
                <div className="text-muted-foreground">加载中...</div>
              </div>
            ) : filteredProviders.length === 0 ? (
              <div className="flex items-center justify-center h-32">
                <div className="text-muted-foreground">
                  {searchQuery ? '未找到匹配的供应商或模型' : '暂无可用的供应商'}
                </div>
              </div>
            ) : (
              <div className="space-y-3">
                {filteredProviders.map((provider) => (
                  <Card key={provider.id}>
                    <CardContent className="p-4">
                      {/* 供应商头部 */}
                      <div 
                        className="flex items-center justify-between cursor-pointer"
                        onClick={() => toggleProvider(provider.id)}
                      >
                        <div className="flex items-center gap-3">
                          {provider.expanded ? (
                            <ChevronDown className="h-4 w-4" />
                          ) : (
                            <ChevronRight className="h-4 w-4" />
                          )}
                          <div>
                            <h3 className="font-medium">{provider.name}</h3>
                            <p className="text-sm text-muted-foreground">
                              {provider.type} • {provider.vendorModels.length} 个模型
                            </p>
                          </div>
                        </div>
                        <Badge 
                          variant={provider.enabled ? "default" : "secondary"}
                          className="text-xs"
                        >
                          {provider.enabled ? '启用' : '禁用'}
                        </Badge>
                      </div>

                      {/* VendorModel列表 */}
                      {provider.expanded && provider.vendorModels.length > 0 && (
                        <div className="mt-4 divide-y rounded-md border bg-background">
                          {provider.vendorModels.map((vendorModel, idx) => {
                            const isBound = isVendorModelBound(vendorModel.id)
                            return (
                              <div
                                key={vendorModel.id}
                                className="flex items-center justify-between gap-4 p-3 hover:bg-muted/50 transition-colors"
                              >
                                <div className="flex-1 min-w-0">
                                  <div className="flex items-center gap-2">
                                    <h4 className="font-medium text-sm truncate">
                                      {vendorModel.vendorModelName}
                                    </h4>
                                    <Badge variant="outline" className="text-xs">
                                      {vendorModel.modelKey}
                                    </Badge>
                                    {isBound && (
                                      <Badge variant="default" className="text-xs">
                                        已绑定
                                      </Badge>
                                    )}
                                    {!vendorModel.enabled && (
                                      <Badge variant="secondary" className="text-xs">已禁用</Badge>
                                    )}
                                  </div>
                                  {vendorModel.description && (
                                    <p className="text-xs text-muted-foreground mt-1 line-clamp-2">
                                      {vendorModel.description}
                                    </p>
                                  )}
                                  {vendorModel.inputPerMillionTokens && (
                                    <p className="text-xs text-muted-foreground mt-1">
                                      输入: {vendorModel.inputPerMillionTokens} {vendorModel.currency || 'USD'}/1M tokens
                                    </p>
                                  )}
                                </div>
                                <Button
                                  size="sm"
                                  variant={isBound ? "outline" : "default"}
                                  onClick={() => toggleVendorModel(vendorModel)}
                                  disabled={!vendorModel.enabled}
                                >
                                  {isBound ? (
                                    <>
                                      <Unlink className="h-3 w-3 mr-1" />
                                      解绑
                                    </>
                                  ) : (
                                    <>
                                      <Link className="h-3 w-3 mr-1" />
                                      绑定
                                    </>
                                  )}
                                </Button>
                              </div>
                            )
                          })}
                        </div>
                      )}

                      {provider.expanded && provider.vendorModels.length === 0 && (
                        <div className="mt-4 text-center text-sm text-muted-foreground">
                          该供应商暂无可用模型
                        </div>
                      )}
                    </CardContent>
                  </Card>
                ))}
              </div>
            )}
          </ScrollArea>
        </div>

        {/* 底部默认 Dialog 会有关闭图标，这里不再重复放置按钮 */}
      </DialogContent>
    </Dialog>
  )
}
'use client'

import { useState, useEffect } from 'react'
import { VendorModel, CreateVendorModelRequest, UpdateVendorModelRequest } from '@/types'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import { Label } from '@/components/ui/label'
import { Switch } from '@/components/ui/switch'
import { ScrollArea } from '@/components/ui/scroll-area'
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogTrigger } from '@/components/ui/dialog'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Bot, Plus, Edit } from 'lucide-react'

export type ProviderType = 'NONE' | 'OPENAI' | 'VOLCANOENGINE' | 'ZHIPU' | 'DASHSCOPE'

interface VendorModelModalProps {
  vendorModel?: VendorModel
  providerId: number
  onSubmit: (data: CreateVendorModelRequest | UpdateVendorModelRequest) => void
  isOpen?: boolean
  onClose?: () => void
}

export function VendorModelModal({
  vendorModel,
  providerId,
  onSubmit,
  isOpen,
  onClose
}: VendorModelModalProps) {
  const [formData, setFormData] = useState({
    vendorModelName: '',
    vendorModelKey: '',
    description: '',
    inputPerMillionTokens: '',
    outputPerMillionTokens: '',
    currency: '',
    cachedInputPrice: '',
    cachedOutputPrice: '',
    weight: '',
    providerType: 'OPENAI' as ProviderType,
    enabled: true
  })
  const [open, setOpen] = useState(isOpen || false)

  // Reset form when modal closes (for new models)
  const handleOpenChange = (newOpen: boolean) => {
    setOpen(newOpen)
    if (!newOpen && !vendorModel) {
      // Reset form when closing modal for creating new model
      setFormData({
        vendorModelName: '',
        vendorModelKey: '',
        description: '',
        inputPerMillionTokens: '',
        outputPerMillionTokens: '',
        currency: '',
        cachedInputPrice: '',
        cachedOutputPrice: '',
        weight: '',
        providerType: 'OPENAI' as ProviderType,
        enabled: true
      })
    }
    if (!newOpen) {
      onClose?.()
    }
  }

  // Update form data when vendorModel changes
  useEffect(() => {
    if (vendorModel) {
      setFormData({
        vendorModelName: vendorModel.vendorModelName || '',
        vendorModelKey: vendorModel.vendorModelKey || '',
        description: vendorModel.description || '',
        inputPerMillionTokens: vendorModel.inputPerMillionTokens || '',
        outputPerMillionTokens: vendorModel.outputPerMillionTokens || '',
        currency: vendorModel.currency || '',
        cachedInputPrice: vendorModel.cachedInputPrice || '',
        cachedOutputPrice: vendorModel.cachedOutputPrice || '',
        weight: vendorModel.weight?.toString() || '',
        providerType: (vendorModel.providerType as ProviderType) || 'OPENAI',
        enabled: vendorModel.enabled ?? true
      })
    } else {
      // Reset form when no vendorModel (for creating new)
      setFormData({
        vendorModelName: '',
        vendorModelKey: '',
        description: '',
        inputPerMillionTokens: '',
        outputPerMillionTokens: '',
        currency: '',
        cachedInputPrice: '',
        cachedOutputPrice: '',
        weight: '',
        providerType: 'OPENAI' as ProviderType,
        enabled: true
      })
    }
  }, [vendorModel])

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()

    const data = vendorModel
      ? {
          providerId: vendorModel.providerId,
          providerType: formData.providerType,
          vendorModelName: formData.vendorModelName,
          vendorModelKey: formData.vendorModelKey,
          description: formData.description || undefined,
          inputPerMillionTokens: formData.inputPerMillionTokens || undefined,
          outputPerMillionTokens: formData.outputPerMillionTokens || undefined,
          currency: formData.currency || undefined,
          cachedInputPrice: formData.cachedInputPrice || undefined,
          cachedOutputPrice: formData.cachedOutputPrice || undefined,
          weight: formData.weight ? parseFloat(formData.weight) : undefined,
          enabled: formData.enabled
        } as UpdateVendorModelRequest
      : {
          providerId,
          providerType: formData.providerType,
          vendorModelName: formData.vendorModelName,
          vendorModelKey: formData.vendorModelKey,
          description: formData.description || undefined,
          inputPerMillionTokens: formData.inputPerMillionTokens || undefined,
          outputPerMillionTokens: formData.outputPerMillionTokens || undefined,
          currency: formData.currency || undefined,
          cachedInputPrice: formData.cachedInputPrice || undefined,
          cachedOutputPrice: formData.cachedOutputPrice || undefined,
          weight: formData.weight ? parseFloat(formData.weight) : undefined,
          enabled: formData.enabled
        } as CreateVendorModelRequest

    onSubmit(data)
    
    // Clear form data immediately after submission for new models
    if (!vendorModel) {
      setFormData({
        vendorModelName: '',
        vendorModelKey: '',
        description: '',
        inputPerMillionTokens: '',
        outputPerMillionTokens: '',
        currency: '',
        cachedInputPrice: '',
        cachedOutputPrice: '',
        weight: '',
        providerType: 'NONE' as ProviderType,
        enabled: true
      })
    }
    
    setOpen(false)
  }

  const handleCancel = () => {
    setOpen(false)
  }

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogTrigger asChild>
        {vendorModel ? (
          <Button variant="outline" size="sm">
            <Edit className="h-4 w-4" />
          </Button>
        ) : (
          <Button size="sm">
            <Plus className="h-4 w-4 mr-1" />
            添加模型
          </Button>
        )}
      </DialogTrigger>
      <DialogContent className="sm:max-w-[425px] max-h-[80vh] flex flex-col">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <Bot className="h-5 w-5" />
            {vendorModel ? '编辑 Vendor Model' : '添加 Vendor Model'}
          </DialogTitle>
        </DialogHeader>
        <ScrollArea className="h-[60vh]">
          <form onSubmit={handleSubmit} className="space-y-4 pr-4">
          <div className="space-y-2">
            <Label htmlFor="vendorModelName">模型名称 *</Label>
            <Input
              id="vendorModelName"
              value={formData.vendorModelName}
              onChange={(e) => setFormData({ ...formData, vendorModelName: e.target.value })}
              placeholder="例如: gpt-4, claude-3-sonnet-20240229"
              required
            />
          </div>

          <div className="space-y-2">
            <Label htmlFor="modelKey">模型调用标识 *</Label>
            <Input
              id="vendorModelKey"
              value={formData.vendorModelKey}
              onChange={(e) => setFormData({ ...formData, vendorModelKey: e.target.value })}
              placeholder="例如: gpt-4, claude-3-sonnet-20240229"
              required
            />
          </div>

          <div className="space-y-2">
            <Label htmlFor="providerType">提供商类型 *</Label>
            <Select value={formData.providerType} onValueChange={(value) => setFormData({ ...formData, providerType: value as ProviderType })}>
              <SelectTrigger>
                <SelectValue placeholder="选择提供商类型" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="NONE">无转换器</SelectItem>
                <SelectItem value="OPENAI">OpenAI</SelectItem>
                <SelectItem value="VOLCANOENGINE">火山引擎</SelectItem>
                <SelectItem value="ZHIPU">智谱AI</SelectItem>
                <SelectItem value="DASHSCOPE">通义千问</SelectItem>
              </SelectContent>
            </Select>
          </div>

          <div className="space-y-2">
            <Label htmlFor="description">描述</Label>
            <Textarea
              id="description"
              value={formData.description}
              onChange={(e) => setFormData({ ...formData, description: e.target.value })}
              placeholder="模型描述..."
              rows={3}
            />
          </div>

          <div className="space-y-2">
            <Label htmlFor="inputPerMillionTokens">输入价格 (每百万tokens)</Label>
            <Input
              id="inputPerMillionTokens"
              value={formData.inputPerMillionTokens}
              onChange={(e) => setFormData({ ...formData, inputPerMillionTokens: e.target.value })}
              placeholder="例如: 0.01"
            />
          </div>

          <div className="space-y-2">
            <Label htmlFor="outputPerMillionTokens">输出价格 (每百万tokens)</Label>
            <Input
              id="outputPerMillionTokens"
              value={formData.outputPerMillionTokens}
              onChange={(e) => setFormData({ ...formData, outputPerMillionTokens: e.target.value })}
              placeholder="例如: 0.03"
            />
          </div>

          <div className="space-y-2">
            <Label htmlFor="currency">货币</Label>
            <Input
              id="currency"
              value={formData.currency}
              onChange={(e) => setFormData({ ...formData, currency: e.target.value })}
              placeholder="例如: USD"
            />
          </div>

          <div className="space-y-2">
            <Label htmlFor="cachedInputPrice">缓存输入价格</Label>
            <Input
              id="cachedInputPrice"
              value={formData.cachedInputPrice}
              onChange={(e) => setFormData({ ...formData, cachedInputPrice: e.target.value })}
              placeholder="例如: 0.005"
            />
          </div>

          <div className="space-y-2">
            <Label htmlFor="cachedOutputPrice">缓存输出价格</Label>
            <Input
              id="cachedOutputPrice"
              value={formData.cachedOutputPrice}
              onChange={(e) => setFormData({ ...formData, cachedOutputPrice: e.target.value })}
              placeholder="例如: 0.015"
            />
          </div>

          <div className="space-y-2">
            <Label htmlFor="weight">权重</Label>
            <Input
              id="weight"
              type="number"
              min="0"
              step="0.1"
              value={formData.weight}
              onChange={(e) => setFormData({ ...formData, weight: e.target.value })}
              placeholder="例如: 1.0"
            />
          </div>

          <div className="flex items-center space-x-2">
            <Switch
              id="enabled"
              checked={formData.enabled}
              onCheckedChange={(checked) => setFormData({ ...formData, enabled: checked })}
            />
            <Label htmlFor="enabled">启用</Label>
          </div>

          <div className="flex justify-end space-x-2 pt-4">
            <Button type="button" variant="outline" onClick={handleCancel}>
              取消
            </Button>
            <Button type="submit">
              {vendorModel ? '更新' : '创建'}
            </Button>
          </div>
        </form>
        </ScrollArea>
      </DialogContent>
    </Dialog>
  )
}
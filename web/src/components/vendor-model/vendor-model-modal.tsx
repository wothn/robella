'use client'

import { useState, useEffect } from 'react'
import { VendorModel, CreateVendorModelRequest, UpdateVendorModelRequest, PricingTier } from '@/types'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import { Label } from '@/components/ui/label'
import { Switch } from '@/components/ui/switch'
import { ScrollArea } from '@/components/ui/scroll-area'
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogTrigger } from '@/components/ui/dialog'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Bot, Plus, Edit, Trash2 } from 'lucide-react'
import { PROVIDER_TYPE_LABELS, ProviderType } from '@/constants/provider-constants'

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
    perRequestPrice: '',
    currency: '',
    cachedInputPrice: '',
    pricingStrategy: 'FIXED' as 'FIXED' | 'PER_REQUEST' | 'TIERED',
    weight: '',
    providerType: 'OPENAI' as ProviderType,
    enabled: true
  })

  const [pricingTiers, setPricingTiers] = useState<PricingTier[]>([])

  // 添加新的定价阶梯
  const addPricingTier = () => {
    const newTier: PricingTier = {
      id: Date.now(), // 使用临时ID
      vendorModelId: 0,
      tierNumber: pricingTiers.length + 1,
      minTokens: 0,
      maxTokens: undefined,
      inputPerMillionTokens: '',
      outputPerMillionTokens: '',
      cachedInputPrice: ''
    }
    setPricingTiers([...pricingTiers, newTier])
  }

  // 更新定价阶梯
  const updatePricingTier = (index: number, field: keyof PricingTier, value: string | number) => {
    const updatedTiers = [...pricingTiers]
    updatedTiers[index] = { ...updatedTiers[index], [field]: value }
    setPricingTiers(updatedTiers)
  }

  // 删除定价阶梯
  const removePricingTier = (index: number) => {
    setPricingTiers(pricingTiers.filter((_, i) => i !== index))
  }
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
        perRequestPrice: '',
        currency: '',
        cachedInputPrice: '',
        pricingStrategy: 'FIXED' as 'FIXED' | 'PER_REQUEST' | 'TIERED',
        weight: '',
        providerType: 'OPENAI' as ProviderType,
        enabled: true
      })
      setPricingTiers([])
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
        perRequestPrice: vendorModel.perRequestPrice || '',
        currency: vendorModel.currency || '',
        cachedInputPrice: vendorModel.cachedInputPrice || '',
        pricingStrategy: vendorModel.pricingStrategy || 'FIXED',
        weight: vendorModel.weight?.toString() || '',
        providerType: (vendorModel.providerType as ProviderType) || 'OPENAI',
        enabled: vendorModel.enabled ?? true
      })
      setPricingTiers(vendorModel.pricingTiers || [])
    } else {
      // Reset form when no vendorModel (for creating new)
      setFormData({
        vendorModelName: '',
        vendorModelKey: '',
        description: '',
        inputPerMillionTokens: '',
        outputPerMillionTokens: '',
        perRequestPrice: '',
        currency: '',
        cachedInputPrice: '',
        pricingStrategy: 'FIXED' as 'FIXED' | 'PER_REQUEST' | 'TIERED',
        weight: '',
        providerType: 'OPENAI' as ProviderType,
        enabled: true
      })
      setPricingTiers([])
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
          perRequestPrice: formData.perRequestPrice || undefined,
          currency: formData.currency || undefined,
          cachedInputPrice: formData.pricingStrategy === 'TIERED' ? undefined : (formData.cachedInputPrice || undefined),
          pricingStrategy: formData.pricingStrategy,
          weight: formData.weight ? parseFloat(formData.weight) : undefined,
          enabled: formData.enabled,
          pricingTiers: formData.pricingStrategy === 'TIERED' ? pricingTiers : undefined
        } as UpdateVendorModelRequest
      : {
          providerId,
          providerType: formData.providerType,
          vendorModelName: formData.vendorModelName,
          vendorModelKey: formData.vendorModelKey,
          description: formData.description || undefined,
          inputPerMillionTokens: formData.inputPerMillionTokens || undefined,
          outputPerMillionTokens: formData.outputPerMillionTokens || undefined,
          perRequestPrice: formData.perRequestPrice || undefined,
          currency: formData.currency || undefined,
          cachedInputPrice: formData.pricingStrategy === 'TIERED' ? undefined : (formData.cachedInputPrice || undefined),
          pricingStrategy: formData.pricingStrategy,
          weight: formData.weight ? parseFloat(formData.weight) : undefined,
          enabled: formData.enabled,
          pricingTiers: formData.pricingStrategy === 'TIERED' ? pricingTiers : undefined
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
        perRequestPrice: '',
        currency: '',
        cachedInputPrice: '',
        pricingStrategy: 'FIXED' as 'FIXED' | 'PER_REQUEST' | 'TIERED',
        weight: '',
        providerType: 'NONE' as ProviderType,
        enabled: true
      })
      setPricingTiers([])
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
                {Object.entries(PROVIDER_TYPE_LABELS).map(([value, label]) => (
                  <SelectItem key={value} value={value}>
                    {label}
                  </SelectItem>
                ))}
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
            <Label htmlFor="pricingStrategy">计费策略 *</Label>
            <Select value={formData.pricingStrategy} onValueChange={(value) => setFormData({ ...formData, pricingStrategy: value as 'FIXED' | 'PER_REQUEST' | 'TIERED' })}>
              <SelectTrigger>
                <SelectValue placeholder="选择计费策略" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="FIXED">固定价格</SelectItem>
                <SelectItem value="PER_REQUEST">按次收费</SelectItem>
                <SelectItem value="TIERED">阶梯计费</SelectItem>
              </SelectContent>
            </Select>
          </div>

          {/* 根据计费策略显示不同的价格配置 */}
          {formData.pricingStrategy === 'PER_REQUEST' ? (
            <div className="space-y-2">
              <Label htmlFor="perRequestPrice">单次请求价格</Label>
              <Input
                id="perRequestPrice"
                value={formData.perRequestPrice}
                onChange={(e) => setFormData({ ...formData, perRequestPrice: e.target.value })}
                placeholder="例如: 0.1"
              />
            </div>
          ) : formData.pricingStrategy === 'TIERED' ? (
            <div className="space-y-4">
              <div className="flex items-center justify-between">
                <Label className="text-sm font-medium">阶梯定价配置</Label>
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  onClick={addPricingTier}
                  className="flex items-center gap-1"
                >
                  <Plus className="h-3 w-3" />
                  添加阶梯
                </Button>
              </div>

              {pricingTiers.map((tier, index) => (
                <div key={tier.id} className="space-y-2 p-3 border rounded-lg">
                  <div className="flex items-center justify-between">
                    <Label className="text-sm font-medium">阶梯 {tier.tierNumber}</Label>
                    <Button
                      type="button"
                      variant="ghost"
                      size="sm"
                      onClick={() => removePricingTier(index)}
                      className="text-destructive hover:text-destructive"
                    >
                      <Trash2 className="h-3 w-3" />
                    </Button>
                  </div>

                  <div className="grid grid-cols-2 gap-2">
                    <div>
                      <Label htmlFor={`minTokens-${index}`} className="text-xs">最小Token数</Label>
                      <Input
                        id={`minTokens-${index}`}
                        type="number"
                        min="0"
                        value={tier.minTokens}
                        onChange={(e) => updatePricingTier(index, 'minTokens', parseInt(e.target.value) || 0)}
                        placeholder="0"
                      />
                    </div>
                    <div>
                      <Label htmlFor={`maxTokens-${index}`} className="text-xs">最大Token数 (可选)</Label>
                      <Input
                        id={`maxTokens-${index}`}
                        type="number"
                        min="0"
                        value={tier.maxTokens || ''}
                        onChange={(e) => updatePricingTier(index, 'maxTokens', e.target.value ? parseInt(e.target.value) : undefined)}
                        placeholder="留空表示无上限"
                      />
                    </div>
                  </div>

                  <div className="grid grid-cols-3 gap-2">
                    <div>
                      <Label htmlFor={`inputPrice-${index}`} className="text-xs">输入价格/百万Token</Label>
                      <Input
                        id={`inputPrice-${index}`}
                        value={tier.inputPerMillionTokens}
                        onChange={(e) => updatePricingTier(index, 'inputPerMillionTokens', e.target.value)}
                        placeholder="0.01"
                      />
                    </div>
                    <div>
                      <Label htmlFor={`outputPrice-${index}`} className="text-xs">输出价格/百万Token</Label>
                      <Input
                        id={`outputPrice-${index}`}
                        value={tier.outputPerMillionTokens}
                        onChange={(e) => updatePricingTier(index, 'outputPerMillionTokens', e.target.value)}
                        placeholder="0.03"
                      />
                    </div>
                    <div>
                      <Label htmlFor={`cachePrice-${index}`} className="text-xs">缓存价格/百万Token</Label>
                      <Input
                        id={`cachePrice-${index}`}
                        value={tier.cachedInputPrice}
                        onChange={(e) => updatePricingTier(index, 'cachedInputPrice', e.target.value)}
                        placeholder="0.005"
                      />
                    </div>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            /* 固定价格 */
            <>
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
            </>
          )}

          <div className="space-y-2">
            <Label htmlFor="currency">货币</Label>
            <Input
              id="currency"
              value={formData.currency}
              onChange={(e) => setFormData({ ...formData, currency: e.target.value })}
              placeholder="例如: USD"
            />
          </div>

          {formData.pricingStrategy === 'FIXED' && (
            <div className="space-y-2">
              <Label htmlFor="cachedInputPrice">缓存输入价格</Label>
              <Input
                id="cachedInputPrice"
                value={formData.cachedInputPrice}
                onChange={(e) => setFormData({ ...formData, cachedInputPrice: e.target.value })}
                placeholder="例如: 0.005"
              />
            </div>
          )}

          


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
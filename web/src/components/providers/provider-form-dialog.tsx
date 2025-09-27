'use client'

import { useState, useEffect } from 'react'
import { Provider } from '@/types'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Switch } from '@/components/ui/switch'
import { Textarea } from '@/components/ui/textarea'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from '@/components/ui/dialog'

export type EndpointType = 'OPENAI' | 'ANTHROPIC'
export type ProviderType = 'NONE' | 'OPENAI' | 'VOLCANOENGINE' | 'ZHIPU' | 'DASHSCOPE'

interface ProviderFormDialogProps {
  isOpen: boolean
  onOpenChange: (open: boolean) => void
  provider?: Provider | null
  onSubmit: (formData: ProviderFormData) => void
  title: string
  description: string
}

export interface ProviderFormData {
  name: string
  endpointType: EndpointType
  providerType: ProviderType
  baseUrl?: string
  apiKey?: string
  enabled: boolean
  config: Record<string, unknown>
}

export function ProviderFormDialog({
  isOpen,
  onOpenChange,
  provider,
  onSubmit,
  title,
  description
}: ProviderFormDialogProps) {
  const [formData, setFormData] = useState<ProviderFormData>({
    name: '',
    endpointType: 'OPENAI',
    providerType: 'OPENAI',
    baseUrl: '',
    apiKey: '',
    enabled: true,
    config: {}
  })

  const [configString, setConfigString] = useState('{}')

  useEffect(() => {
    let parsedConfig: Record<string, unknown> = {}
    let configStr = '{}'

    if (provider?.config) {
      try {
        parsedConfig = JSON.parse(provider.config)
        configStr = JSON.stringify(parsedConfig, null, 2)
      } catch {
        configStr = provider.config
        try {
          parsedConfig = JSON.parse(configStr)
        } catch {
          // If parsing fails, use empty object
        }
      }
    }

    setFormData({
      name: provider?.name || '',
      endpointType: (provider?.endpointType as EndpointType) || 'OPENAI',
      providerType: (provider?.providerType as ProviderType) || 'OPENAI',
      baseUrl: provider?.baseUrl || '',
      apiKey: provider?.apiKey || '',
      enabled: provider?.enabled ?? true,
      config: parsedConfig
    })
    setConfigString(configStr)
  }, [provider])

  const handleSubmit = () => {
    try {
      let configObj: Record<string, unknown> = {}
      if (configString.trim()) {
        configObj = JSON.parse(configString)
      }
      onSubmit({
        ...formData,
        config: configObj
      })
    } catch {
      alert('配置格式错误，请确保是有效的JSON格式')
    }
  }

  return (
    <Dialog open={isOpen} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-[500px]">
        <DialogHeader>
          <DialogTitle className="text-lg font-semibold">{title}</DialogTitle>
          <DialogDescription className="text-sm text-muted-foreground">
            {description}
          </DialogDescription>
        </DialogHeader>
        <div className="space-y-4 py-4">
          <div className="space-y-2">
            <Label htmlFor="name" className="text-sm font-medium">名称 *</Label>
            <Input
              id="name"
              value={formData.name}
              onChange={(e) => setFormData(prev => ({ ...prev, name: e.target.value }))}
              placeholder="输入提供商名称"
              required
            />
          </div>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label htmlFor="endpointType" className="text-sm font-medium">端点类型 *</Label>
              <Select value={formData.endpointType} onValueChange={(value) => setFormData(prev => ({ ...prev, endpointType: value as EndpointType }))}>
                <SelectTrigger>
                  <SelectValue placeholder="选择端点类型" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="OPENAI">OpenAI</SelectItem>
                  <SelectItem value="ANTHROPIC">Anthropic</SelectItem>
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-2">
              <Label htmlFor="providerType" className="text-sm font-medium">提供商类型 *</Label>
              <Select value={formData.providerType} onValueChange={(value) => setFormData(prev => ({ ...prev, providerType: value as ProviderType }))}>
                <SelectTrigger>
                  <SelectValue placeholder="选择提供商类型" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="NONE">无转换器</SelectItem>
                  <SelectItem value="OPENAI">OPENAI</SelectItem>
                  <SelectItem value="VOLCANOENGINE">火山引擎</SelectItem>
                  <SelectItem value="ZHIPU">智谱AI</SelectItem>
                  <SelectItem value="DASHSCOPE">通义千问</SelectItem>
                </SelectContent>
              </Select>
            </div>
          </div>
          <div className="space-y-2">
            <Label htmlFor="baseUrl" className="text-sm font-medium">Base URL</Label>
            <Input
              id="baseUrl"
              value={formData.baseUrl}
              onChange={(e) => setFormData(prev => ({ ...prev, baseUrl: e.target.value }))}
              placeholder="https://api.example.com"
            />
          </div>
          <div className="space-y-2">
            <Label htmlFor="apiKey" className="text-sm font-medium">API Key</Label>
            <Input
              id="apiKey"
              type="password"
              value={formData.apiKey}
              onChange={(e) => setFormData(prev => ({ ...prev, apiKey: e.target.value }))}
              placeholder="输入API密钥"
            />
          </div>
          <div className="flex items-center justify-between">
            <Label htmlFor="enabled" className="text-sm font-medium">启用</Label>
            <Switch
              id="enabled"
              checked={formData.enabled}
              onCheckedChange={(checked: boolean) => setFormData(prev => ({ ...prev, enabled: checked }))}
            />
          </div>
          <div className="space-y-2">
            <Label htmlFor="config" className="text-sm font-medium">配置 (JSON)</Label>
            <Textarea
              id="config"
              value={configString}
              onChange={(e) => setConfigString(e.target.value)}
              placeholder='{ "timeout": 30000, "maxRetries": 3 }'
              className="font-mono text-sm"
              rows={4}
            />
          </div>
        </div>
        <div className="flex justify-end space-x-2 pt-4">
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            取消
          </Button>
          <Button onClick={handleSubmit}>
            {provider ? '保存' : '创建'}
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  )
}
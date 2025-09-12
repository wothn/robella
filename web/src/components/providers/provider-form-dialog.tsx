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
  type: string
  baseUrl?: string
  apiKey?: string
  enabled: boolean
  config: Record<string, any>
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
    type: '',
    baseUrl: '',
    apiKey: '',
    enabled: true,
    config: {}
  })

  const [configString, setConfigString] = useState('{}')

  useEffect(() => {
    let parsedConfig = {}
    let configStr = '{}'
    
    if (provider?.config) {
      try {
        parsedConfig = JSON.parse(provider.config)
        configStr = JSON.stringify(parsedConfig, null, 2)
      } catch (e) {
        configStr = provider.config
        try {
          parsedConfig = JSON.parse(configStr)
        } catch (e2) {
          // If parsing fails, use empty object
        }
      }
    }
    
    setFormData({
      name: provider?.name || '',
      type: provider?.type || '',
      baseUrl: provider?.baseUrl || '',
      apiKey: provider?.apiKey || '',
      enabled: provider?.enabled ?? true,
      config: parsedConfig
    })
    setConfigString(configStr)
  }, [provider])

  const handleSubmit = () => {
    try {
      let configObj = {}
      if (configString.trim()) {
        configObj = JSON.parse(configString)
      }
      onSubmit({
        ...formData,
        config: configObj
      })
    } catch (e) {
      alert('配置格式错误，请确保是有效的JSON格式')
    }
  }

  return (
    <Dialog open={isOpen} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-[425px]">
        <DialogHeader>
          <DialogTitle>{title}</DialogTitle>
          <DialogDescription>
            {description}
          </DialogDescription>
        </DialogHeader>
        <div className="grid gap-4 py-4">
          <div className="grid grid-cols-4 items-center gap-4">
            <Label htmlFor="name" className="text-right">名称</Label>
            <Input
              id="name"
              value={formData.name}
              onChange={(e) => setFormData(prev => ({ ...prev, name: e.target.value }))}
              className="col-span-3"
            />
          </div>
          <div className="grid grid-cols-4 items-center gap-4">
            <Label htmlFor="type" className="text-right">类型</Label>
            <Select value={formData.type} onValueChange={(value) => setFormData(prev => ({ ...prev, type: value }))}>
              <SelectTrigger className="col-span-3">
                <SelectValue placeholder="选择类型" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="openai">OpenAI</SelectItem>
                <SelectItem value="anthropic">Anthropic</SelectItem>
                <SelectItem value="gemini">Gemini</SelectItem>
                <SelectItem value="qwen">Qwen</SelectItem>
                <SelectItem value="custom">Custom</SelectItem>
              </SelectContent>
            </Select>
          </div>
          <div className="grid grid-cols-4 items-center gap-4">
            <Label htmlFor="baseUrl" className="text-right">Base URL</Label>
            <Input
              id="baseUrl"
              value={formData.baseUrl}
              onChange={(e) => setFormData(prev => ({ ...prev, baseUrl: e.target.value }))}
              className="col-span-3"
              placeholder="https://api.example.com"
            />
          </div>
          <div className="grid grid-cols-4 items-center gap-4">
            <Label htmlFor="apiKey" className="text-right">API Key</Label>
            <Input
              id="apiKey"
              type="password"
              value={formData.apiKey}
              onChange={(e) => setFormData(prev => ({ ...prev, apiKey: e.target.value }))}
              className="col-span-3"
            />
          </div>
          <div className="grid grid-cols-4 items-center gap-4">
            <Label htmlFor="enabled" className="text-right">启用</Label>
            <Switch
              id="enabled"
              checked={formData.enabled}
              onCheckedChange={(checked: boolean) => setFormData(prev => ({ ...prev, enabled: checked }))}
            />
          </div>
          <div className="grid grid-cols-4 items-center gap-4">
            <Label htmlFor="config" className="text-right">配置</Label>
            <Textarea
              id="config"
              value={configString}
              onChange={(e) => setConfigString(e.target.value)}
              className="col-span-3"
              placeholder="JSON格式配置"
              rows={3}
            />
          </div>
        </div>
        <div className="flex justify-end space-x-2">
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
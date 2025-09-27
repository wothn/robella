'use client'

import { Provider } from '@/types'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Label } from '@/components/ui/label'
import { Edit, Trash2 } from 'lucide-react'

interface ProviderBasicInfoProps {
  provider: Provider
  onEdit: () => void
  onDelete: (providerId: number) => void
}

export function ProviderBasicInfo({ provider, onEdit, onDelete }: ProviderBasicInfoProps) {
  return (
    <div className="flex items-center justify-between mb-6">
      <div className="space-y-1">
        <h1 className="text-2xl font-bold tracking-tight">{provider.name}</h1>
        <p className="text-sm text-muted-foreground">{provider.endpointType} • {provider.providerType}</p>
      </div>
      <div className="flex items-center gap-2">
        <Button variant="outline" size="sm" onClick={onEdit}>
          <Edit className="h-4 w-4 mr-2" />
          编辑
        </Button>
        <Button variant="destructive" size="sm" onClick={() => onDelete(provider.id)}>
          <Trash2 className="h-4 w-4 mr-2" />
          删除
        </Button>
      </div>
    </div>
  )
}

interface ProviderDetailsCardProps {
  provider: Provider
}

export function ProviderDetailsCard({ provider }: ProviderDetailsCardProps) {
  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-lg font-semibold">基本信息</CardTitle>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div className="space-y-2">
            <Label className="text-sm font-medium text-muted-foreground">名称</Label>
            <p className="text-sm font-medium">{provider.name}</p>
          </div>
          <div className="space-y-2">
            <Label className="text-sm font-medium text-muted-foreground">端点类型</Label>
            <p className="text-sm font-medium">{provider.endpointType}</p>
          </div>
          <div className="space-y-2">
            <Label className="text-sm font-medium text-muted-foreground">提供商类型</Label>
            <p className="text-sm font-medium">{provider.providerType}</p>
          </div>
          <div className="space-y-2">
            <Label className="text-sm font-medium text-muted-foreground">状态</Label>
            <Badge variant={provider.enabled ? 'default' : 'secondary'}>
              {provider.enabled ? '启用' : '禁用'}
            </Badge>
          </div>
          <div className="space-y-2 md:col-span-2">
            <Label className="text-sm font-medium text-muted-foreground">Base URL</Label>
            <p className="text-sm font-medium break-all text-foreground">{provider.baseUrl || '未设置'}</p>
          </div>
        </div>
      </CardContent>
    </Card>
  )
}

interface ProviderConfigCardProps {
  provider: Provider
}

export function ProviderConfigCard({ provider }: ProviderConfigCardProps) {
  const formatConfig = (config: string) => {
    try {
      const parsed = JSON.parse(config);
      return JSON.stringify(parsed, null, 2);
    } catch {
      return config;
    }
  };

  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-lg font-semibold">配置信息</CardTitle>
      </CardHeader>
      <CardContent>
        {provider.config ? (
          <pre className="bg-muted p-4 rounded-md text-sm overflow-x-auto font-mono">
            {formatConfig(provider.config)}
          </pre>
        ) : (
          <p className="text-muted-foreground text-sm">无额外配置信息</p>
        )}
      </CardContent>
    </Card>
  )
}
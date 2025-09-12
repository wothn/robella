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
  onDelete: () => void
}

export function ProviderBasicInfo({ provider, onEdit, onDelete }: ProviderBasicInfoProps) {
  return (
    <div className="flex items-center justify-between mb-6">
      <div>
        <h1 className="text-2xl font-bold">{provider.name}</h1>
        <p className="text-gray-500">{provider.type}</p>
      </div>
      <div className="flex items-center space-x-2">
        <Button variant="outline" size="sm" onClick={onEdit}>
          <Edit className="h-4 w-4 mr-1" />
          编辑
        </Button>
        <Button variant="destructive" size="sm" onClick={onDelete}>
          <Trash2 className="h-4 w-4 mr-1" />
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
        <CardTitle>基本信息</CardTitle>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="grid grid-cols-2 gap-4">
          <div>
            <Label className="text-sm font-medium">名称</Label>
            <p className="text-sm text-gray-600">{provider.name}</p>
          </div>
          <div>
            <Label className="text-sm font-medium">类型</Label>
            <p className="text-sm text-gray-600">{provider.type}</p>
          </div>
          <div>
            <Label className="text-sm font-medium">状态</Label>
            <Badge variant={provider.enabled ? 'default' : 'secondary'}>
              {provider.enabled ? '启用' : '禁用'}
            </Badge>
          </div>
          <div>
            <Label className="text-sm font-medium">Base URL</Label>
            <p className="text-sm text-gray-600 break-all">{provider.baseUrl || '未设置'}</p>
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
  return (
    <Card>
      <CardHeader>
        <CardTitle>配置信息</CardTitle>
      </CardHeader>
      <CardContent>
        {provider.config ? (() => {
          try {
            return (
              <pre className="bg-gray-100 p-4 rounded-lg text-sm overflow-x-auto">
                {JSON.stringify(JSON.parse(provider.config), null, 2)}
              </pre>
            )
          } catch (e) {
            return (
              <pre className="bg-gray-100 p-4 rounded-lg text-sm overflow-x-auto">
                {provider.config}
              </pre>
            )
          }
        })() : (
          <p className="text-gray-500">无额外配置信息</p>
        )}
      </CardContent>
    </Card>
  )
}
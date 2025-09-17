'use client'

import { VendorModel } from '@/types'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Bot, Trash2 } from 'lucide-react'
import { VendorModelModal } from '@/components/providers/vendor-model-modal'

interface VendorModelsListProps {
  vendorModels: VendorModel[]
  providerId: number
  onUpdateModel: (modelId: number, data: any) => Promise<void>
  onDeleteModel: (modelId: number) => void
}

export function VendorModelsList({
  vendorModels,
  providerId,
  onUpdateModel,
  onDeleteModel
}: VendorModelsListProps) {

  if (vendorModels.length === 0) {
    return (
      <Card>
        <CardContent className="flex flex-col items-center justify-center py-12">
          <Bot className="h-12 w-12 text-gray-400 mb-4" />
          <h3 className="text-lg font-medium text-gray-900 mb-2">暂无模型</h3>
          <p className="text-gray-500 text-center">
            该 Provider 还没有添加任何模型
          </p>
        </CardContent>
      </Card>
    )
  }

  return (
    <>
      <div className="grid gap-4">
        {vendorModels.map((model) => (
          <Card key={model.id}>
            <CardContent className="p-4">
              <div className="flex items-center justify-between">
                <div className="flex-1">
                  <div className="flex items-center space-x-2 mb-2">
                    <h4 className="font-medium">{model.vendorModelName}</h4>
                    <Badge variant="outline">
                      {model.vendorModelKey}
                    </Badge>
                    <Badge variant={model.enabled ? 'default' : 'secondary'}>
                      {model.enabled ? '启用' : '禁用'}
                    </Badge>
                    {model.providerType && (
                      <Badge variant="outline">
                        {model.providerType === 'NONE' ? '无转换器' :
                         model.providerType === 'OPENAI' ? 'OpenAI' :
                         model.providerType === 'VOLCANOENGINE' ? '火山引擎' :
                         model.providerType === 'ZHIPU' ? '智谱AI' :
                         model.providerType === 'DASHSCOPE' ? '通义千问' : model.providerType}
                      </Badge>
                    )}
                  </div>
                  {model.description && (
                    <p className="text-sm text-gray-600 mb-2">{model.description}</p>
                  )}
                  {(model.inputPerMillionTokens || model.outputPerMillionTokens) && (
                    <div className="text-xs text-gray-500">
                      <strong>定价:</strong> 
                      {model.inputPerMillionTokens && (
                        <span>输入: {parseFloat(model.inputPerMillionTokens).toFixed(4)} {model.currency || 'USD'}/M tokens</span>
                      )}
                      {model.outputPerMillionTokens && (
                        <span> 输出: {parseFloat(model.outputPerMillionTokens).toFixed(4)} {model.currency || 'USD'}/M tokens</span>
                      )}
                      {model.cachedInputPrice && (
                        <span> 缓存输入: {parseFloat(model.cachedInputPrice).toFixed(4)} {model.currency || 'USD'}</span>
                      )}
                      {model.cachedOutputPrice && (
                        <span> 缓存输出: {parseFloat(model.cachedOutputPrice).toFixed(4)} {model.currency || 'USD'}</span>
                      )}
                    </div>
                  )}
                </div>
                <div className="flex items-center space-x-2">
                  <VendorModelModal
                    vendorModel={model}
                    providerId={providerId}
                    onSubmit={(data) => onUpdateModel(model.id, data)}
                  />
                  <Button
                    variant="destructive"
                    size="sm"
                    onClick={() => onDeleteModel(model.id)}
                  >
                    <Trash2 className="h-4 w-4" />
                  </Button>
                </div>
              </div>
            </CardContent>
          </Card>
        ))}
      </div>
    </>
  )
}
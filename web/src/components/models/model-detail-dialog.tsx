'use client'

import { useState, useEffect } from 'react'
import { Settings, Eye, Calendar, Cpu, Layers, DollarSign } from 'lucide-react'
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import {
  Tabs,
  TabsContent,
  TabsList,
  TabsTrigger,
} from '@/components/ui/tabs'
import { Badge } from '@/components/ui/badge'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'
import { Separator } from '@/components/ui/separator'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { api } from '@/lib/api'
import type { Model, ModelCapability } from '@/types/model'
import type { VendorModel } from '@/types/vendor-model'
import type { Provider } from '@/types/provider'
import { formatDistanceToNow } from 'date-fns'
import { zhCN } from 'date-fns/locale'

interface ModelDetailDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  modelId: number | null
}

// 能力标签颜色映射
const capabilityColors: Record<ModelCapability, string> = {
  TEXT: 'bg-blue-100 text-blue-800',
  VISION: 'bg-green-100 text-green-800',
  REASONING: 'bg-purple-100 text-purple-800',
  FUNCTION_CALLING: 'bg-orange-100 text-orange-800',
  EMBEDDING: 'bg-pink-100 text-pink-800',
  WEB_SEARCH: 'bg-yellow-100 text-yellow-800',
  RERANKING: 'bg-gray-100 text-gray-800'
}

// 能力中文名称映射
const capabilityNames: Record<ModelCapability, string> = {
  TEXT: '文本处理',
  VISION: '视觉理解',
  REASONING: '推理分析',
  FUNCTION_CALLING: '函数调用',
  EMBEDDING: '向量嵌入',
  WEB_SEARCH: '网络搜索',
  RERANKING: '重排序'
}

export function ModelDetailDialog({ open, onOpenChange, modelId }: ModelDetailDialogProps) {
  const [model, setModel] = useState<Model | null>(null)
  const [vendorModels, setVendorModels] = useState<VendorModel[]>([])
  const [providers, setProviders] = useState<Provider[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [activeTab, setActiveTab] = useState('details')

  // 加载模型详情和相关数据
  useEffect(() => {
    if (open && modelId) {
      loadModelDetails()
    }
  }, [open, modelId])

  const loadModelDetails = async () => {
    if (!modelId) return

    setLoading(true)
    setError(null)

    try {
      // 并行加载数据
      const [modelData, vendorModelsData, providersData] = await Promise.all([
        api.getModelById(modelId),
        api.getVendorModelsByModelId(modelId),
        api.getAllProviders()
      ])

      setModel(modelData)
      setVendorModels(vendorModelsData)
      setProviders(providersData)
    } catch (err) {
      setError(err instanceof Error ? err.message : '加载模型详情失败')
      console.error('加载模型详情失败:', err)
    } finally {
      setLoading(false)
    }
  }

  // 格式化时间
  const formatTime = (dateString?: string) => {
    if (!dateString) return ''
    try {
      return formatDistanceToNow(new Date(dateString), {
        addSuffix: true,
        locale: zhCN
      })
    } catch {
      return dateString
    }
  }

  // 根据providerId获取provider名称
  const getProviderName = (providerId: number): string => {
    const provider = providers.find(p => p.id === providerId)
    return provider?.name || `Provider ${providerId}`
  }

  // 重置状态
  const handleClose = () => {
    setModel(null)
    setVendorModels([])
    setProviders([])
    setError(null)
    setActiveTab('details')
    onOpenChange(false)
  }

  return (
    <Dialog open={open} onOpenChange={handleClose}>
      <DialogContent className="max-w-4xl max-h-[90vh] overflow-hidden">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <Eye className="h-5 w-5" />
            模型详情
          </DialogTitle>
        </DialogHeader>

        {loading && (
          <div className="space-y-4 p-4">
            <div className="flex items-center gap-4">
              <Skeleton className="h-8 w-48" />
              <Skeleton className="h-6 w-20" />
            </div>
            <Skeleton className="h-4 w-full" />
            <Skeleton className="h-4 w-3/4" />
            <div className="grid grid-cols-3 gap-4">
              <Skeleton className="h-24" />
              <Skeleton className="h-24" />
              <Skeleton className="h-24" />
            </div>
          </div>
        )}

        {error && (
          <Alert variant="destructive">
            <AlertDescription>{error}</AlertDescription>
          </Alert>
        )}

        {model && !loading && (
          <Tabs value={activeTab} onValueChange={setActiveTab} className="w-full">
            <TabsList className="grid w-full grid-cols-2">
              <TabsTrigger value="details" className="flex items-center gap-2">
                <Settings className="h-4 w-4" />
                基本信息
              </TabsTrigger>
              <TabsTrigger value="vendors" className="flex items-center gap-2">
                <Layers className="h-4 w-4" />
                供应商模型
                <Badge variant="secondary" className="ml-1">
                  {vendorModels.length}
                </Badge>
              </TabsTrigger>
            </TabsList>

            <TabsContent value="details" className="space-y-6 mt-6">
              {/* 模型基本信息 */}
              <div className="space-y-4">
                <div className="flex items-start justify-between">
                  <div className="space-y-2">
                    <h2 className="text-2xl font-bold">{model.name}</h2>
                    {model.organization && (
                      <p className="text-muted-foreground">{model.organization}</p>
                    )}
                  </div>
                  <Badge 
                    variant={model.published ? 'default' : 'secondary'}
                    className="shrink-0"
                  >
                    {model.published ? '已发布' : '草稿'}
                  </Badge>
                </div>

                {model.description && (
                  <p className="text-muted-foreground leading-relaxed">
                    {model.description}
                  </p>
                )}
              </div>

              <Separator />

              {/* 能力和规格 */}
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                {/* 模型能力 */}
                <Card>
                  <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                      <Cpu className="h-5 w-5" />
                      模型能力
                    </CardTitle>
                  </CardHeader>
                  <CardContent>
                    {model.capabilities && model.capabilities.length > 0 ? (
                      <div className="flex flex-wrap gap-2">
                        {model.capabilities.map((capability) => (
                          <Badge 
                            key={capability} 
                            variant="outline" 
                            className={capabilityColors[capability]}
                          >
                            {capabilityNames[capability]}
                          </Badge>
                        ))}
                      </div>
                    ) : (
                      <p className="text-muted-foreground">未设置能力信息</p>
                    )}
                  </CardContent>
                </Card>

                {/* 技术规格 */}
                <Card>
                  <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                      <Settings className="h-5 w-5" />
                      技术规格
                    </CardTitle>
                  </CardHeader>
                  <CardContent className="space-y-2">
                    <div className="flex justify-between">
                      <span className="text-muted-foreground">上下文窗口:</span>
                      <span className="font-mono">
                        {model.contextWindow ? model.contextWindow.toLocaleString() : '未设置'}
                      </span>
                    </div>
                  </CardContent>
                </Card>
              </div>

              {/* 时间信息 */}
              <Card>
                <CardHeader>
                  <CardTitle className="flex items-center gap-2">
                    <Calendar className="h-5 w-5" />
                    时间信息
                  </CardTitle>
                </CardHeader>
                <CardContent className="space-y-2">
                  <div className="flex justify-between">
                    <span className="text-muted-foreground">创建时间:</span>
                    <span>{formatTime(model.createdAt)}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-muted-foreground">更新时间:</span>
                    <span>{formatTime(model.updatedAt)}</span>
                  </div>
                </CardContent>
              </Card>
            </TabsContent>

            <TabsContent value="vendors" className="mt-6">
              {vendorModels.length > 0 ? (
                <div className="space-y-4">
                  <div className="flex items-center justify-between">
                    <h3 className="text-lg font-semibold">关联的供应商模型</h3>
                    <Badge variant="outline">
                      {vendorModels.length} 个模型
                    </Badge>
                  </div>
                  
                  <div className="grid gap-4">
                    {vendorModels.map((vendorModel) => (
                      <Card key={vendorModel.id}>
                        <CardContent className="p-4">
                          <div className="flex items-start justify-between">
                            <div className="space-y-2">
                              <div className="flex items-center gap-2">
                                <h4 className="font-medium">{vendorModel.vendorModelName}</h4>
                                <Badge variant="outline" className="text-xs">
                                  {vendorModel.modelKey}
                                </Badge>
                                <Badge
                                  variant={vendorModel.enabled ? 'default' : 'secondary'}
                                  className="text-xs"
                                >
                                  {vendorModel.enabled ? '启用' : '禁用'}
                                </Badge>
                              </div>
                              
                              <p className="text-sm text-muted-foreground">
                                供应商: {getProviderName(vendorModel.providerId)}
                              </p>
                              
                              {vendorModel.description && (
                                <p className="text-sm text-muted-foreground">
                                  {vendorModel.description}
                                </p>
                              )}
                            </div>
                            
                            <div className="text-right space-y-1">
                              {vendorModel.inputPerMillionTokens && (
                                <div className="flex items-center gap-1">
                                  <DollarSign className="h-3 w-3" />
                                  <span className="text-xs text-muted-foreground">
                                    输入: {vendorModel.inputPerMillionTokens} {vendorModel.currency || 'USD'}
                                  </span>
                                </div>
                              )}
                              {vendorModel.outputPerMillionTokens && (
                                <div className="flex items-center gap-1">
                                  <DollarSign className="h-3 w-3" />
                                  <span className="text-xs text-muted-foreground">
                                    输出: {vendorModel.outputPerMillionTokens} {vendorModel.currency || 'USD'}
                                  </span>
                                </div>
                              )}
                            </div>
                          </div>
                        </CardContent>
                      </Card>
                    ))}
                  </div>
                </div>
              ) : (
                <Card>
                  <CardContent className="flex flex-col items-center justify-center py-12">
                    <Layers className="h-12 w-12 text-muted-foreground mb-4" />
                    <p className="text-muted-foreground text-center">
                      暂无关联的供应商模型
                    </p>
                    <p className="text-sm text-muted-foreground text-center mt-1">
                      您可以在供应商管理页面配置模型关联
                    </p>
                  </CardContent>
                </Card>
              )}
            </TabsContent>
          </Tabs>
        )}
      </DialogContent>
    </Dialog>
  )
}
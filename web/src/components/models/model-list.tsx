import { useState } from 'react'
import { 
  MoreHorizontal, 
  Eye, 
  EyeOff, 
  Edit, 
  Trash2, 
  Settings,
  ExternalLink 
} from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { 
  DropdownMenu, 
  DropdownMenuContent, 
  DropdownMenuItem, 
  DropdownMenuSeparator, 
  DropdownMenuTrigger 
} from '@/components/ui/dropdown-menu'
import { 
  AlertDialog, 
  AlertDialogAction, 
  AlertDialogCancel, 
  AlertDialogContent, 
  AlertDialogDescription, 
  AlertDialogFooter, 
  AlertDialogHeader, 
  AlertDialogTitle 
} from '@/components/ui/alert-dialog'
import { Skeleton } from '@/components/ui/skeleton'
import { ModelFormDialog } from './model-form-dialog'
import { useModels } from '@/hooks/use-models'
import type { Model, ModelCapability } from '@/types/model'
import { formatDistanceToNow } from 'date-fns'
import { zhCN } from 'date-fns/locale'

interface ModelListProps {
  models: Model[]
  loading: boolean
  error: string | null
  onRefresh: () => void
  onViewDetails: (modelId: number) => void
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

export function ModelList({ models, loading, error, onRefresh, onViewDetails }: ModelListProps) {
  const [editingModel, setEditingModel] = useState<Model | null>(null)
  const [deleteModelId, setDeleteModelId] = useState<number | null>(null)
  const { publishModel, unpublishModel, deleteModel } = useModels()

  // 处理发布/取消发布
  const handleTogglePublish = async (model: Model) => {
    try {
      if (model.published) {
        await unpublishModel(model.id)
      } else {
        await publishModel(model.id)
      }
      onRefresh()
    } catch (error) {
      console.error('切换发布状态失败:', error)
    }
  }

  // 处理删除
  const handleDelete = async () => {
    if (!deleteModelId) return
    
    try {
      await deleteModel(deleteModelId)
      setDeleteModelId(null)
      onRefresh()
    } catch (error) {
      console.error('删除模型失败:', error)
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

  // 加载状态
  if (loading) {
    return (
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
        {Array.from({ length: 6 }).map((_, i) => (
          <Card key={i}>
            <CardHeader>
              <div className="flex justify-between items-start">
                <div className="space-y-2">
                  <Skeleton className="h-5 w-[150px]" />
                  <Skeleton className="h-4 w-[100px]" />
                </div>
                <Skeleton className="h-8 w-8 rounded" />
              </div>
            </CardHeader>
            <CardContent className="space-y-3">
              <Skeleton className="h-4 w-full" />
              <Skeleton className="h-4 w-3/4" />
              <div className="flex gap-2">
                <Skeleton className="h-6 w-16" />
                <Skeleton className="h-6 w-16" />
              </div>
            </CardContent>
          </Card>
        ))}
      </div>
    )
  }

  // 错误状态
  if (error) {
    return (
      <Card>
        <CardContent className="flex flex-col items-center justify-center py-12">
          <p className="text-red-500 mb-4">{error}</p>
          <Button onClick={onRefresh}>重试</Button>
        </CardContent>
      </Card>
    )
  }

  // 空状态
  if (models.length === 0) {
    return (
      <Card>
        <CardContent className="flex flex-col items-center justify-center py-12">
          <p className="text-muted-foreground mb-4">暂无模型数据</p>
          <Button onClick={onRefresh}>刷新</Button>
        </CardContent>
      </Card>
    )
  }

  return (
    <>
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
        {models.map((model) => (
          <Card 
            key={model.id} 
            className="group hover:shadow-md transition-shadow cursor-pointer"
            onClick={() => onViewDetails(model.id)}
          >
            <CardHeader>
              <div className="flex justify-between items-start">
                <div className="space-y-1">
                  <CardTitle className="text-lg">{model.name}</CardTitle>
                  <CardDescription className="flex items-center gap-2">
                    {model.organization && (
                      <span className="text-sm">{model.organization}</span>
                    )}
                    <Badge 
                      variant={model.published ? 'default' : 'secondary'}
                      className="text-xs"
                    >
                      {model.published ? '已发布' : '草稿'}
                    </Badge>
                  </CardDescription>
                </div>
                <DropdownMenu>
                  <DropdownMenuTrigger asChild>
                    <Button 
                      variant="ghost" 
                      size="sm" 
                      className="opacity-0 group-hover:opacity-100 transition-opacity"
                      onClick={(e) => e.stopPropagation()}
                    >
                      <MoreHorizontal className="h-4 w-4" />
                    </Button>
                  </DropdownMenuTrigger>
                  <DropdownMenuContent align="end">
                    <DropdownMenuItem onClick={() => setEditingModel(model)}>
                      <Edit className="h-4 w-4 mr-2" />
                      编辑
                    </DropdownMenuItem>
                    <DropdownMenuItem onClick={() => handleTogglePublish(model)}>
                      {model.published ? (
                        <>
                          <EyeOff className="h-4 w-4 mr-2" />
                          取消发布
                        </>
                      ) : (
                        <>
                          <Eye className="h-4 w-4 mr-2" />
                          发布
                        </>
                      )}
                    </DropdownMenuItem>
                    <DropdownMenuItem>
                      <Settings className="h-4 w-4 mr-2" />
                      配置
                    </DropdownMenuItem>
                    <DropdownMenuItem onClick={() => onViewDetails(model.id)}>
                      <ExternalLink className="h-4 w-4 mr-2" />
                      详情
                    </DropdownMenuItem>
                    <DropdownMenuSeparator />
                    <DropdownMenuItem 
                      onClick={() => setDeleteModelId(model.id)}
                      className="text-red-600"
                    >
                      <Trash2 className="h-4 w-4 mr-2" />
                      删除
                    </DropdownMenuItem>
                  </DropdownMenuContent>
                </DropdownMenu>
              </div>
            </CardHeader>
            <CardContent className="space-y-3">
              {model.description && (
                <p className="text-sm text-muted-foreground line-clamp-2">
                  {model.description}
                </p>
              )}
              
              {model.capabilities && model.capabilities.length > 0 && (
                <div className="flex flex-wrap gap-1">
                  {model.capabilities.slice(0, 3).map((capability) => (
                    <Badge 
                      key={capability} 
                      variant="outline" 
                      className={`text-xs ${capabilityColors[capability]}`}
                    >
                      {capabilityNames[capability]}
                    </Badge>
                  ))}
                  {model.capabilities.length > 3 && (
                    <Badge variant="outline" className="text-xs">
                      +{model.capabilities.length - 3}
                    </Badge>
                  )}
                </div>
              )}
              
              <div className="flex justify-between items-center text-xs text-muted-foreground">
                <span>
                  {model.contextWindow && `上下文: ${model.contextWindow.toLocaleString()}`}
                </span>
                <span>
                  {formatTime(model.updatedAt)}
                </span>
              </div>
            </CardContent>
          </Card>
        ))}
      </div>

      {/* 编辑对话框 */}
      <ModelFormDialog
        open={!!editingModel}
        onOpenChange={(open) => !open && setEditingModel(null)}
        model={editingModel}
        onSuccess={() => {
          setEditingModel(null)
          onRefresh()
        }}
      />

      {/* 删除确认对话框 */}
      <AlertDialog open={!!deleteModelId} onOpenChange={() => setDeleteModelId(null)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>确认删除</AlertDialogTitle>
            <AlertDialogDescription>
              此操作将永久删除该模型，且无法撤销。确定要继续吗？
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>取消</AlertDialogCancel>
            <AlertDialogAction onClick={handleDelete} className="bg-red-600 hover:bg-red-700">
              删除
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </>
  )
}

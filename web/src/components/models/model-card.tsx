import { memo } from 'react'
import {
  MoreHorizontal,
  Eye,
  EyeOff,
  Edit,
  Trash2,
  Settings,
  ExternalLink,
  Copy,
  Check
} from 'lucide-react'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger
} from '@/components/ui/dropdown-menu'
import dayjs from 'dayjs'
import relativeTime from 'dayjs/plugin/relativeTime'
import 'dayjs/locale/zh-cn'
import type { Model } from '@/types/model'
import { CAPABILITY_NAMES, CAPABILITY_COLORS } from '@/constants/model-constants'

dayjs.extend(relativeTime)
dayjs.locale('zh-cn')

interface ModelCardProps {
  model: Model
  bindingsCount: number
  onViewDetails: (modelId: number) => void
  onEdit: (model: Model) => void
  onTogglePublish: (model: Model) => void
  onConfigure: (model: Model) => void
  onDelete: (model: Model) => void
  onCopyModelKey: (modelKey: string, e: React.MouseEvent) => void
  copiedModelKey: string | null
}

export const ModelCard = memo(({
  model,
  bindingsCount,
  onViewDetails,
  onEdit,
  onTogglePublish,
  onConfigure,
  onDelete,
  onCopyModelKey,
  copiedModelKey
}: ModelCardProps) => {
  const formatTime = (dateString?: string) => {
    if (!dateString) return ''
    try {
      return dayjs(dateString).fromNow()
    } catch {
      return dateString
    }
  }

  return (
    <Card className="group hover:shadow-md transition-shadow cursor-pointer">
      <CardHeader>
        <div className="flex justify-between items-start">
          <div className="space-y-1">
            <CardTitle className="text-lg">{model.name}</CardTitle>
            <CardDescription className="flex flex-col gap-1">
              <div
                className="text-xs font-mono bg-muted px-2 py-1 rounded cursor-pointer hover:bg-muted/80 transition-colors flex items-center gap-1 group"
                onClick={(e) => onCopyModelKey(model.modelKey, e)}
                data-no-card-click="true"
                title="点击复制"
              >
                <span className="truncate">{model.modelKey}</span>
                {copiedModelKey === model.modelKey ? (
                  <Check className="h-3 w-3 text-green-600 flex-shrink-0" />
                ) : (
                  <Copy className="h-3 w-3 opacity-0 group-hover:opacity-100 transition-opacity flex-shrink-0" />
                )}
              </div>
              {model.organization && (
                <span className="text-sm">{model.organization}</span>
              )}
              <Badge
                variant={model.published ? 'default' : 'secondary'}
                className="text-xs"
              >
                {model.published ? '已发布' : '草稿'}
              </Badge>
              {bindingsCount > 0 && (
                <Badge variant="outline" className="text-xs">
                  {bindingsCount} 个绑定
                </Badge>
              )}
            </CardDescription>
          </div>
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button
                variant="ghost"
                size="sm"
                className="opacity-0 group-hover:opacity-100 transition-opacity"
                data-no-card-click="true"
              >
                <MoreHorizontal className="h-4 w-4" />
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent
              align="end"
              className="w-48 min-w-[8rem]"
            >
              <DropdownMenuItem data-no-card-click="true" onClick={() => onEdit(model)}>
                <Edit className="h-4 w-4 mr-2" />
                编辑
              </DropdownMenuItem>
              <DropdownMenuItem data-no-card-click="true" onClick={() => onTogglePublish(model)}>
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
              <DropdownMenuItem data-no-card-click="true" onClick={() => onConfigure(model)}>
                <Settings className="h-4 w-4 mr-2" />
                配置
              </DropdownMenuItem>
              <DropdownMenuItem data-no-card-click="true" onClick={() => onViewDetails(model.id)}>
                <ExternalLink className="h-4 w-4 mr-2" />
                详情
              </DropdownMenuItem>
              <DropdownMenuSeparator />
              <DropdownMenuItem
                data-no-card-click="true"
                onClick={() => onDelete(model)}
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
                className={`text-xs ${CAPABILITY_COLORS[capability]}`}
              >
                {CAPABILITY_NAMES[capability]}
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
  )
})

ModelCard.displayName = 'ModelCard'
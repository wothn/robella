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
    <Card className="group hover:shadow-lg transition-all duration-300 cursor-pointer border-0 shadow-sm hover:shadow-xl hover:-translate-y-0.5 bg-gradient-to-br from-background to-muted/20 backdrop-blur-sm">
      <CardHeader className="pb-3">
        <div className="flex justify-between items-start">
          <div className="space-y-2">
            <div className="flex items-center gap-2">
              <CardTitle className="text-lg font-semibold tracking-tight">{model.name}</CardTitle>
              <Badge
                variant={model.published ? 'default' : 'secondary'}
                className="text-xs px-2 py-0.5 rounded-full"
              >
                {model.published ? '已发布' : '草稿'}
              </Badge>
            </div>
            <CardDescription className="flex flex-col gap-2">
              <div
                className="text-xs font-mono bg-muted/60 px-3 py-1.5 rounded-lg cursor-pointer hover:bg-muted/80 transition-all duration-200 flex items-center gap-2 group border border-border/50"
                onClick={(e) => onCopyModelKey(model.modelKey, e)}
                data-no-card-click="true"
                title="点击复制"
              >
                <span className="truncate font-medium">{model.modelKey}</span>
                {copiedModelKey === model.modelKey ? (
                  <Check className="h-3.5 w-3.5 text-green-600 flex-shrink-0" />
                ) : (
                  <Copy className="h-3.5 w-3.5 opacity-60 group-hover:opacity-100 transition-opacity flex-shrink-0" />
                )}
              </div>
              {model.organization && (
                <span className="text-sm text-muted-foreground flex items-center gap-1">
                  <span className="w-1 h-1 bg-muted-foreground/50 rounded-full"></span>
                  {model.organization}
                </span>
              )}
              <div className="flex items-center gap-2">
                {bindingsCount > 0 && (
                  <Badge variant="outline" className="text-xs bg-background/50">
                    {bindingsCount} 个绑定
                  </Badge>
                )}
              </div>
            </CardDescription>
          </div>
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button
                variant="ghost"
                size="sm"
                className="opacity-0 group-hover:opacity-100 transition-all duration-200 hover:bg-accent/50 data-[state=open]:opacity-100"
                data-no-card-click="true"
              >
                <MoreHorizontal className="h-4 w-4" />
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent
              align="end"
              className="w-48 min-w-[8rem] backdrop-blur-sm border-border/50"
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
                className="text-red-600 hover:text-red-700 hover:bg-red-50 dark:hover:bg-red-950/20"
              >
                <Trash2 className="h-4 w-4 mr-2" />
                删除
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        </div>
      </CardHeader>
      <CardContent className="space-y-4 min-h-[120px]">
        {model.description && (
          <p className="text-sm text-muted-foreground line-clamp-2 leading-relaxed">
            {model.description}
          </p>
        )}

        {model.capabilities && model.capabilities.length > 0 && (
          <div className="flex flex-wrap gap-1.5">
            {model.capabilities.slice(0, 3).map((capability) => (
              <Badge
                key={capability}
                variant="outline"
                className={`text-xs bg-background/50 hover:bg-background/80 transition-colors ${CAPABILITY_COLORS[capability]}`}
              >
                {CAPABILITY_NAMES[capability]}
              </Badge>
            ))}
            {model.capabilities.length > 3 && (
              <Badge variant="outline" className="text-xs bg-background/50">
                +{model.capabilities.length - 3}
              </Badge>
            )}
          </div>
        )}

        <div className="flex justify-between items-center text-xs text-muted-foreground pt-2 border-t border-border/50">
          <span className="flex items-center gap-1">
            <span className="w-1 h-1 bg-muted-foreground/50 rounded-full"></span>
            {model.contextWindow && `上下文: ${model.contextWindow.toLocaleString()}`}
          </span>
          <span className="font-medium">
            {formatTime(model.updatedAt)}
          </span>
        </div>
      </CardContent>
    </Card>
  )
})

ModelCard.displayName = 'ModelCard'
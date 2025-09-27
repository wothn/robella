import { memo } from 'react'
import { MoreHorizontal, Eye, EyeOff, Edit, Trash2, Settings, ExternalLink } from 'lucide-react'
import { Button } from '@/components/ui/button'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger
} from '@/components/ui/dropdown-menu'
import type { Model } from '@/types/model'

interface ModelActionsProps {
  model: Model
  onEdit: (model: Model) => void
  onTogglePublish: (model: Model) => void
  onConfigure: (model: Model) => void
  onViewDetails: (modelId: number) => void
  onDelete: (model: Model) => void
}

export const ModelActions = memo(({
  model,
  onEdit,
  onTogglePublish,
  onConfigure,
  onViewDetails,
  onDelete
}: ModelActionsProps) => {
  return (
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
  )
})

ModelActions.displayName = 'ModelActions'
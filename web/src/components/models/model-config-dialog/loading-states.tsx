'use client'

interface LoadingStateProps {
  message?: string
}

export function LoadingState({ message = '加载中...' }: LoadingStateProps) {
  return (
    <div className="flex items-center justify-center py-8">
      <div className="text-muted-foreground">{message}</div>
    </div>
  )
}

interface EmptyStateProps {
  hasSearchQuery: boolean
}

export function EmptyState({ hasSearchQuery }: EmptyStateProps) {
  return (
    <div className="flex items-center justify-center py-8">
      <div className="text-muted-foreground">
        {hasSearchQuery ? '未找到匹配的供应商或模型' : '暂无可用的供应商'}
      </div>
    </div>
  )
}
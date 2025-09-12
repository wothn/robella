import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'
import { BarChart3, Eye, FileText, TrendingUp } from 'lucide-react'
import type { ModelStats as ModelStatsType } from '@/types/model'

interface ModelStatsProps {
  stats: ModelStatsType | null
  loading: boolean
}

export function ModelStats({ stats, loading }: ModelStatsProps) {
  if (loading) {
    return (
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
        {Array.from({ length: 4 }).map((_, i) => (
          <Card key={i}>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <Skeleton className="h-4 w-[100px]" />
              <Skeleton className="h-4 w-4 rounded" />
            </CardHeader>
            <CardContent>
              <Skeleton className="h-7 w-[60px] mb-1" />
              <Skeleton className="h-3 w-[120px]" />
            </CardContent>
          </Card>
        ))}
      </div>
    )
  }

  if (!stats) {
    return null
  }

  const statCards = [
    {
      title: '总模型数',
      value: stats.totalModels,
      description: '系统中的所有模型',
      icon: BarChart3,
      trend: '+12% 较上月'
    },
    {
      title: '已发布模型',
      value: stats.publishedModels,
      description: '当前可用的模型',
      icon: Eye,
      trend: '+8% 较上月'
    },
    {
      title: '草稿模型',
      value: stats.totalModels - stats.publishedModels,
      description: '未发布的模型',
      icon: FileText,
      trend: '+4% 较上月'
    },
    {
      title: '发布率',
      value: stats.totalModels === 0 ? '0%' : `${Math.round((stats.publishedModels / stats.totalModels) * 100)}%`,
      description: '模型发布比例',
      icon: TrendingUp,
      trend: '+2% 较上月'
    }
  ]

  return (
    <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
      {statCards.map((stat, index) => {
        const Icon = stat.icon
        return (
          <Card key={index}>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">
                {stat.title}
              </CardTitle>
              <Icon className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{stat.value}</div>
              <p className="text-xs text-muted-foreground">
                {stat.description}
              </p>
            </CardContent>
          </Card>
        )
      })}
    </div>
  )
}

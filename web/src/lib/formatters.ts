/**
 * 货币格式化工具
 */
export function formatCurrency(
  amount: number,
  currency: string = 'USD',
  locale: string = 'en-US'
): string {
  try {
    return new Intl.NumberFormat(locale, {
      style: 'currency',
      currency: currency,
      minimumFractionDigits: 4,
      maximumFractionDigits: 4,
    }).format(amount)
  } catch {
    // 如果Intl.NumberFormat不支持该货币，回退到基本格式化
    return `${amount.toFixed(4)} ${currency}`
  }
}

/**
 * 格式化百分比
 */
export function formatPercentage(
  value: number,
  decimals: number = 2
): string {
  return `${value.toFixed(decimals)}%`
}

/**
 * 格式化数字（添加千位分隔符）
 */
export function formatNumber(
  value: number,
  decimals: number = 2
): string {
  return new Intl.NumberFormat('en-US', {
    minimumFractionDigits: decimals,
    maximumFractionDigits: decimals,
  }).format(value)
}

/**
 * 格式化文件大小
 */
export function formatFileSize(bytes: number): string {
  if (bytes === 0) return '0 Bytes'
  
  const k = 1024
  const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

/**
 * 格式化日期时间
 */
export function formatDateTime(
  date: string | Date,
  locale: string = 'zh-CN'
): string {
  const dateObj = typeof date === 'string' ? new Date(date) : date
  
  try {
    return new Intl.DateTimeFormat(locale, {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
    }).format(dateObj)
  } catch {
    // 如果Intl.DateTimeFormat不支持该locale，回退到ISO字符串
    return dateObj.toISOString()
  }
}

/**
 * 格式化相对时间
 */
export function formatRelativeTime(
  date: string | Date,
  locale: string = 'zh-CN'
): string {
  const dateObj = typeof date === 'string' ? new Date(date) : date
  const now = new Date()
  const diffInSeconds = Math.floor((now.getTime() - dateObj.getTime()) / 1000)

  if (diffInSeconds < 60) return '刚刚'
  if (diffInSeconds < 3600) return `${Math.floor(diffInSeconds / 60)} 分钟前`
  if (diffInSeconds < 86400) return `${Math.floor(diffInSeconds / 3600)} 小时前`
  if (diffInSeconds < 2592000) return `${Math.floor(diffInSeconds / 86400)} 天前`

  return formatDateTime(dateObj, locale)
}

/**
 * 格式化价格显示
 */
export function formatPriceDisplay(
  price: string | undefined,
  currency: string = 'USD'
): string | null {
  if (!price) return null
  const numPrice = parseFloat(price)
  if (isNaN(numPrice)) return null
  return `${formatCurrency(numPrice, currency)} /1M`
}

/**
 * 格式化权重显示
 */
export function formatWeightDisplay(weight: string | number | undefined): string | null {
  if (weight == null) return null
  const numWeight = typeof weight === 'string' ? parseFloat(weight) : weight
  if (isNaN(numWeight)) return null
  return numWeight.toFixed(1)
}

/**
 * 获取提供商类型标签
 */
export function getProviderTypeLabel(providerType: string): string {
  const labels: Record<string, string> = {
    'NONE': '无转换器',
    'OPENAI': 'OpenAI',
    'VOLCANOENGINE': '火山引擎',
    'ZHIPU': '智谱AI',
    'DASHSCOPE': '通义千问'
  }
  return labels[providerType] || providerType
}
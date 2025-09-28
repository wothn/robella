/**
 * Provider type labels mapping
 */
export const PROVIDER_TYPE_LABELS: Record<string, string> = {
  'NONE': '无转换器',
  'OPENAI': 'OpenAI',
  'VOLCANOENGINE': '火山引擎',
  'ZHIPU': '智谱AI',
  'DASHSCOPE': '通义千问'
}

/**
 * Endpoint type labels mapping
 */
export const ENDPOINT_TYPE_LABELS: Record<string, string> = {
  'OPENAI': 'OpenAI',
  'ANTHROPIC': 'Anthropic'
}

/**
 * Sort options for models
 */
export const MODEL_SORT_OPTIONS = [
  { value: 'name', label: '名称' },
  { value: 'price', label: '价格' },
  { value: 'weight', label: '权重' },
  { value: 'status', label: '状态' }
] as const

/**
 * Provider types
 */
export type ProviderType = 'NONE' | 'OPENAI' | 'VOLCANOENGINE' | 'ZHIPU' | 'DASHSCOPE'

/**
 * Endpoint types
 */
export type EndpointType = 'OPENAI' | 'ANTHROPIC'

/**
 * Sort options
 */
export type ModelSortOption = 'name' | 'price' | 'weight' | 'status'

/**
 * Sort order
 */
export type SortOrder = 'asc' | 'desc'
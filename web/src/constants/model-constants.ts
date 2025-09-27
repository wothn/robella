import type { ModelCapability } from '@/types/model'

export const MODEL_CAPABILITIES: ModelCapability[] = [
  'TEXT',
  'VISION',
  'REASONING',
  'FUNCTION_CALLING',
  'EMBEDDING',
  'WEB_SEARCH',
  'RERANKING'
]

export const CAPABILITY_NAMES: Record<ModelCapability, string> = {
  TEXT: '文本处理',
  VISION: '视觉理解',
  REASONING: '推理分析',
  FUNCTION_CALLING: '函数调用',
  EMBEDDING: '向量嵌入',
  WEB_SEARCH: '网络搜索',
  RERANKING: '重排序'
}

export const CAPABILITY_COLORS: Record<ModelCapability, string> = {
  TEXT: 'bg-blue-100 text-blue-800',
  VISION: 'bg-green-100 text-green-800',
  REASONING: 'bg-purple-100 text-purple-800',
  FUNCTION_CALLING: 'bg-orange-100 text-orange-800',
  EMBEDDING: 'bg-pink-100 text-pink-800',
  WEB_SEARCH: 'bg-yellow-100 text-yellow-800',
  RERANKING: 'bg-gray-100 text-gray-800'
}

export const CAPABILITY_OPTIONS: { value: ModelCapability; label: string }[] = [
  { value: 'TEXT', label: '文本处理' },
  { value: 'VISION', label: '视觉理解' },
  { value: 'REASONING', label: '推理分析' },
  { value: 'FUNCTION_CALLING', label: '函数调用' },
  { value: 'EMBEDDING', label: '向量嵌入' },
  { value: 'WEB_SEARCH', label: '网络搜索' },
  { value: 'RERANKING', label: '重排序' },
]

export const ORGANIZATION_OPTIONS = [
  'OpenAI',
  'Anthropic',
  'Google',
  'Meta',
  'Microsoft',
  'Cohere',
  'Mistral',
  'Hugging Face',
  'Other'
] as const

export type ModelTabType = 'all' | 'published' | 'draft'
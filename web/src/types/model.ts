export type ModelCapability = 
  | 'TEXT'
  | 'VISION'
  | 'REASONING'
  | 'FUNCTION_CALLING'
  | 'EMBEDDING'
  | 'WEB_SEARCH'
  | 'RERANKING'

export interface Model {
  id: number
  name: string
  modelKey: string
  description?: string
  organization?: string
  capabilities?: ModelCapability[]
  contextWindow?: number
  published: boolean
  createdAt?: string
  updatedAt?: string
}

export interface CreateModelRequest {
  name: string
  modelKey: string
  description?: string
  organization?: string
  capabilities?: ModelCapability[]
  contextWindow?: number
  published?: boolean
}

export interface UpdateModelRequest {
  name?: string
  modelKey?: string
  description?: string
  organization?: string
  capabilities?: ModelCapability[]
  contextWindow?: number
  published?: boolean
}

export interface ModelFilters {
  query?: string
  organization?: string
  capability?: string
  published?: boolean
}
export interface Provider {
  id: number
  name: string
  type?: string
  apiKey?: string
  baseUrl?: string
  deploymentName?: string
  apiVersion?: string
  enabled: boolean
  createdAt?: string
  updatedAt?: string
}

export const ModelCapability = {
  TEXT: 'TEXT',
  VISION: 'VISION',
  REASONING: 'REASONING',
  FUNCTION_CALLING: 'FUNCTION_CALLING',
  EMBEDDING: 'EMBEDDING',
  WEB_SEARCH: 'WEB_SEARCH',
  RERANKING: 'RERANKING'
} as const;

export type ModelCapability = typeof ModelCapability[keyof typeof ModelCapability];

export interface Pricing {
  inputPerMillionTokens?: number
  outputPerMillionTokens?: number
  currencySymbol?: string
  cachedInputPrice?: number
  cachedOutputPrice?: number
}

export interface Model {
  id: number
  providerId: number
  name: string
  group?: string
  ownedBy?: string
  description?: string
  capabilities?: string
  inputPerMillionTokens?: number
  outputPerMillionTokens?: number
  currencySymbol?: string
  cachedInputPrice?: number
  cachedOutputPrice?: number
  supportedTextDelta?: boolean
  vendorModel?: string
  enabled: boolean
  createdAt?: string
  updatedAt?: string
  capabilitiesList?: ModelCapability[]
  pricingInfo?: Pricing
}

export interface CreateProviderRequest {
  name: string
  type: string
  apiKey?: string
  baseUrl?: string
  deploymentName?: string
  apiVersion?: string
  enabled: boolean
}

export interface CreateModelRequest {
  name: string
  vendorModel?: string
  enabled: boolean
  description?: string
  capabilities?: string
  inputPerMillionTokens?: number
  outputPerMillionTokens?: number
  currencySymbol?: string
  cachedInputPrice?: number
  cachedOutputPrice?: number
  supportedTextDelta?: boolean
}
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

export enum ModelCapability {
  TEXT,
  VISION,
  REASONING,
  FUNCTION_CALLING,
  EMBEDDING,
  WEB_SEARCH,
  RERANKING
}

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
export interface PricingTier {
  id: number
  vendorModelId: number
  tierNumber: number
  minTokens: number
  maxTokens?: number
  inputPerMillionTokens: string
  outputPerMillionTokens: string
  cachedInputPrice: string
  createdAt?: string
  updatedAt?: string
}

export interface VendorModel {
  id: number
  modelId?: number
  modelKey?: string
  providerId: number
  providerType: string
  vendorModelName: string
  vendorModelKey: string
  description?: string
  inputPerMillionTokens?: string
  outputPerMillionTokens?: string
  perRequestPrice?: string
  currency?: string
  cachedInputPrice?: string
  pricingStrategy?: 'FIXED' | 'PER_REQUEST' | 'TIERED'
  weight?: number
  enabled: boolean
  createdAt?: string
  updatedAt?: string
  pricingTiers?: PricingTier[]
}



export interface CreateVendorModelRequest {
  modelId?: number
  modelKey?: string
  providerId: number
  providerType: string
  vendorModelName: string
  vendorModelKey: string
  description?: string
  inputPerMillionTokens?: string
  outputPerMillionTokens?: string
  perRequestPrice?: string
  currency?: string
  cachedInputPrice?: string
  pricingStrategy?: 'FIXED' | 'PER_REQUEST' | 'TIERED'
  weight?: number
  enabled: boolean
  pricingTiers?: PricingTier[]
}

export interface UpdateVendorModelRequest {
  modelId?: number
  modelKey?: string
  providerId?: number
  providerType?: string
  vendorModelName?: string
  vendorModelKey?: string
  description?: string
  inputPerMillionTokens?: string
  outputPerMillionTokens?: string
  perRequestPrice?: string
  currency?: string
  cachedInputPrice?: string
  pricingStrategy?: 'FIXED' | 'PER_REQUEST' | 'TIERED'
  weight?: number
  enabled?: boolean
  pricingTiers?: PricingTier[]
}
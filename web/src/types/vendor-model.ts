export interface VendorModel {
  id: number
  modelId?: number
  providerId: number
  providerType: string
  vendorModelName: string
  description?: string
  inputPerMillionTokens?: string
  outputPerMillionTokens?: string
  currency?: string
  cachedInputPrice?: string
  cachedOutputPrice?: string
  enabled: boolean
  createdAt?: string
  updatedAt?: string
}



export interface CreateVendorModelRequest {
  modelId?: number
  providerId: number
  providerType: string
  vendorModelName: string
  description?: string
  inputPerMillionTokens?: string
  outputPerMillionTokens?: string
  currency?: string
  cachedInputPrice?: string
  cachedOutputPrice?: string
  enabled: boolean
}

export interface UpdateVendorModelRequest {
  modelId?: number
  providerId?: number
  providerType?: string
  vendorModelName?: string
  description?: string
  inputPerMillionTokens?: string
  outputPerMillionTokens?: string
  currency?: string
  cachedInputPrice?: string
  cachedOutputPrice?: string
  enabled?: boolean
}
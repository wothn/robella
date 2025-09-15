export interface Provider {
  id: number
  name: string
  endpointType: string
  providerType: string
  baseUrl?: string
  apiKey?: string
  config?: string
  enabled: boolean
  createdAt?: string
  updatedAt?: string
}

export interface CreateProviderRequest {
  name: string
  endpointType: string
  providerType: string
  baseUrl?: string
  apiKey?: string
  config?: string
  enabled: boolean
}

export interface UpdateProviderRequest {
  name?: string
  endpointType?: string
  providerType?: string
  baseUrl?: string
  apiKey?: string
  config?: string
  enabled?: boolean
}

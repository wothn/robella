export interface Provider {
  id: number
  name: string
  type: string
  baseUrl?: string
  apiKey?: string
  config?: string
  enabled: boolean
  createdAt?: string
  updatedAt?: string
}

export interface CreateProviderRequest {
  name: string
  type: string
  baseUrl?: string
  apiKey?: string
  config?: string
  enabled: boolean
}

export interface UpdateProviderRequest {
  name?: string
  type?: string
  baseUrl?: string
  apiKey?: string
  config?: string
  enabled?: boolean
}

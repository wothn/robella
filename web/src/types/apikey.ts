export interface ApiKey {
  id: number // JSON will convert Long to number
  name: string
  description: string
  keyPrefix: string
  apiKey?: string // 只在创建时返回
  dailyLimit: number | null
  monthlyLimit: number | null
  rateLimit: number | null
  active: boolean
  lastUsedAt: string | null // ISO date string
  expiresAt: string | null // ISO date string
  createdAt: string // ISO date string
  updatedAt: string // ISO date string
}

export interface ApiKeyCreateRequest {
  name: string
  description: string
  dailyLimit: number | null
  monthlyLimit: number | null
  rateLimit: number | null
}
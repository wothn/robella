import { useState, useEffect } from 'react'
import { api } from '@/lib/api'
import { usePageLoading, withPageLoading, withActionLoading } from '@/stores/loading-store'
import type { ApiKey, ApiKeyCreateRequest } from '@/types/apikey'

export function useApiKeys() {
  const [apiKeys, setApiKeys] = useState<ApiKey[]>([])
  const [error, setError] = useState<string | null>(null)
  const { loading } = usePageLoading('apikeys')

  // 获取所有 API Keys
  const fetchApiKeys = async () => {
    return withPageLoading('apikeys', async () => {
      setError(null)
      try {
        const keys = await api.getUserApiKeys()
        setApiKeys(keys)
      } catch (err) {
        const errorMessage = err instanceof Error ? err.message : 'Failed to fetch API keys'
        setError(errorMessage)
        console.error('Error fetching API keys:', err)
      }
    })
  }

  // 创建新的 API Key
  const createApiKey = async (data: ApiKeyCreateRequest) => {
    return withActionLoading('create-apikey', async () => {
      setError(null)
      try {
        const newKey = await api.createApiKey(data)
        setApiKeys(prev => [newKey, ...prev])
        return newKey
      } catch (err) {
        const errorMessage = err instanceof Error ? err.message : 'Failed to create API key'
        setError(errorMessage)
        console.error('Error creating API key:', err)
        throw err
      }
    })
  }

  // 删除 API Key
  const deleteApiKey = async (id: number) => {
    return withActionLoading('delete-apikey', async () => {
      setError(null)
      try {
        await api.deleteApiKey(id)
        setApiKeys(prev => prev.filter(key => key.id !== id))
      } catch (err) {
        const errorMessage = err instanceof Error ? err.message : 'Failed to delete API key'
        setError(errorMessage)
        console.error('Error deleting API key:', err)
        throw err
      }
    })
  }

  // 切换 API Key 状态
  const toggleApiKeyStatus = async (id: number) => {
    return withActionLoading('toggle-apikey', async () => {
      setError(null)
      try {
        const updatedKey = await api.toggleApiKeyStatus(id)
        setApiKeys(prev => prev.map(key =>
          key.id === id ? updatedKey : key
        ))
        return updatedKey
      } catch (err) {
        const errorMessage = err instanceof Error ? err.message : 'Failed to toggle API key status'
        setError(errorMessage)
        console.error('Error toggling API key status:', err)
        throw err
      }
    })
  }

  // 初始化时获取 API Keys
  useEffect(() => {
    fetchApiKeys()
  }, [])

  return {
    apiKeys,
    loading,
    error,
    fetchApiKeys,
    createApiKey,
    deleteApiKey,
    toggleApiKeyStatus
  }
}
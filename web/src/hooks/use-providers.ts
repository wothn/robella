import { useState, useEffect } from 'react'
import { apiClient } from '@/lib/api'
import { usePageLoading, withPageLoading, withActionLoading } from '@/stores/loading-store'
import { Provider, VendorModel } from '@/types'

export interface ProviderFormData {
  name: string
  endpointType: string
  providerType: string
  baseUrl?: string
  apiKey?: string
  config: Record<string, any>
  enabled: boolean
}

export interface UseProvidersReturn {
  providers: Provider[]
  selectedProvider: Provider | null
  vendorModels: VendorModel[]
  loading: boolean
  error: string | null
  setSelectedProvider: (provider: Provider | null) => void
  createProvider: (formData: ProviderFormData) => Promise<void>
  updateProvider: (id: number, formData: ProviderFormData) => Promise<void>
  deleteProvider: (id: number) => Promise<void>
  refreshProviders: () => Promise<void>
  refreshVendorModels: (providerId: number) => Promise<void>
  clearError: () => void
}

export function useProviders(): UseProvidersReturn {
  const [providers, setProviders] = useState<Provider[]>([])
  const [selectedProvider, setSelectedProvider] = useState<Provider | null>(null)
  const [vendorModels, setVendorModels] = useState<VendorModel[]>([])
  const [error, setError] = useState<string | null>(null)
  const { loading } = usePageLoading('providers')

  const loadProviders = async () => {
    return withPageLoading('providers', async () => {
      try {
        setError(null)
        const data = await apiClient.getAllProviders()
        setProviders(data)
        if (data.length > 0 && !selectedProvider) {
          setSelectedProvider(data[0])
        }
      } catch (err) {
        setError('Failed to load providers')
        console.error('Failed to load providers:', err)
      }
    })
  }

  const loadVendorModels = async (providerId: number) => {
    return withPageLoading('vendor-models', async () => {
      try {
        setError(null)
        const data = await apiClient.getVendorModelsByProviderId(providerId)
        setVendorModels(data)
      } catch (err) {
        setError('Failed to load vendor models')
        console.error('Failed to load vendor models:', err)
      }
    })
  }

  const createProvider = async (formData: ProviderFormData) => {
    return withActionLoading('create-provider', async () => {
      try {
        setError(null)
        const newProvider = await apiClient.createProvider({
          name: formData.name,
          endpointType: formData.endpointType,
          providerType: formData.providerType,
          baseUrl: formData.baseUrl || undefined,
          apiKey: formData.apiKey || undefined,
          config: JSON.stringify(formData.config),
          enabled: formData.enabled
        })

        setProviders(prev => [...prev, newProvider])
        setSelectedProvider(newProvider)
      } catch (err) {
        setError('Failed to create provider')
        console.error('Failed to create provider:', err)
        throw err
      }
    })
  }

  const updateProvider = async (id: number, formData: ProviderFormData) => {
    return withActionLoading('update-provider', async () => {
      try {
        setError(null)
        const updatedProvider = await apiClient.updateProvider(id, {
          name: formData.name,
          endpointType: formData.endpointType,
          providerType: formData.providerType,
          baseUrl: formData.baseUrl || undefined,
          apiKey: formData.apiKey || undefined,
          config: JSON.stringify(formData.config),
          enabled: formData.enabled
        })

        setProviders(prev => prev.map(p => p.id === id ? updatedProvider : p))
        if (selectedProvider?.id === id) {
          setSelectedProvider(updatedProvider)
        }
      } catch (err) {
        setError('Failed to update provider')
        console.error('Failed to update provider:', err)
        throw err
      }
    })
  }

  const deleteProvider = async (id: number) => {
    return withActionLoading('delete-provider', async () => {
      try {
        setError(null)
        await apiClient.deleteProvider(id)
        setProviders(prev => prev.filter(p => p.id !== id))
        if (selectedProvider?.id === id) {
          setSelectedProvider(providers.find(p => p.id !== id) || null)
        }
      } catch (err) {
        setError('Failed to delete provider')
        console.error('Failed to delete provider:', err)
        throw err
      }
    })
  }

  useEffect(() => {
    loadProviders()
  }, [])

  useEffect(() => {
    if (selectedProvider) {
      loadVendorModels(selectedProvider.id)
    } else {
      setVendorModels([])
    }
  }, [selectedProvider])

  return {
    providers,
    selectedProvider,
    vendorModels,
    loading,
    error,
    setSelectedProvider,
    createProvider,
    updateProvider,
    deleteProvider,
    refreshProviders: loadProviders,
    refreshVendorModels: loadVendorModels,
    clearError: () => setError(null)
  }
}
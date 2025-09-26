import { useState, useEffect } from 'react'
import { api } from '@/lib/api'
import { useComponentLoading, withComponentLoading } from '@/stores/loading-store'

interface ModelBindings {
  [modelId: number]: number // 绑定的VendorModel数量
}

export function useModelBindings(modelIds: number[]) {
  const [bindings, setBindings] = useState<ModelBindings>({})
  const { loading } = useComponentLoading('model-bindings')

  const loadBindings = async () => {
    if (modelIds.length === 0) return

    return withComponentLoading('model-bindings', async () => {
      try {
        const bindingsPromises = modelIds.map(async (modelId) => {
          try {
            const vendorModels = await api.getVendorModelsByModelId(modelId)
            return { modelId, count: vendorModels.length }
          } catch (error) {
            console.error(`Failed to load bindings for model ${modelId}:`, error)
            return { modelId, count: 0 }
          }
        })

        const results = await Promise.all(bindingsPromises)
        const newBindings: ModelBindings = {}
        results.forEach(({ modelId, count }) => {
          newBindings[modelId] = count
        })

        setBindings(newBindings)
      } catch (error) {
        console.error('Failed to load model bindings:', error)
      }
    })
  }

  useEffect(() => {
    loadBindings()
  }, [modelIds.join(',')])

  return {
    bindings,
    loading,
    refetch: loadBindings
  }
}
import { useState, useEffect } from 'react'
import { api } from '@/lib/api'

interface ModelBindings {
  [modelId: number]: number // 绑定的VendorModel数量
}

export function useModelBindings(modelIds: number[]) {
  const [bindings, setBindings] = useState<ModelBindings>({})
  const [loading, setLoading] = useState(false)

  const loadBindings = async () => {
    if (modelIds.length === 0) return
    
    setLoading(true)
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
    } finally {
      setLoading(false)
    }
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
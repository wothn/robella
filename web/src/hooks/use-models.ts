import { useState, useEffect, useCallback } from 'react'
import { apiClient } from '@/lib/api'
import { usePageLoading, withPageLoading } from '@/stores/loading-store'
import type { Model, CreateModelRequest, UpdateModelRequest } from '@/types/model'

export function useModels() {
  const [models, setModels] = useState<Model[]>([])
  const [publishedModels, setPublishedModels] = useState<Model[]>([])
  const [error, setError] = useState<string | null>(null)
  const { loading } = usePageLoading('models')

  // 获取所有模型
  const fetchModels = useCallback(async () => {
    return withPageLoading('models', async () => {
      try {
        setError(null)
        const data = await apiClient.getModels()
        setModels(data)
      } catch (err) {
        setError(err instanceof Error ? err.message : '获取模型列表失败')
      }
    })
  }, [])

  // 获取已发布模型
  const fetchPublishedModels = useCallback(async () => {
    try {
      const data = await apiClient.getPublishedModels()
      setPublishedModels(data)
    } catch (err) {
      console.error('获取已发布模型列表失败:', err)
    }
  }, [])



  // 搜索模型
  const searchModels = useCallback(async (keyword: string) => {
    return withPageLoading('models', async () => {
      try {
        setError(null)
        const data = await apiClient.searchModels(keyword)
        setModels(data)
      } catch (err) {
        setError(err instanceof Error ? err.message : '搜索模型失败')
      }
    })
  }, [])

  // 按能力筛选
  const getModelsByCapability = useCallback(async (capability: string) => {
    return withPageLoading('models', async () => {
      try {
        setError(null)
        const data = await apiClient.getModelsByCapability(capability)
        setModels(data)
      } catch (err) {
        setError(err instanceof Error ? err.message : '按能力筛选模型失败')
      }
    })
  }, [])

  // 创建模型
  const createModel = useCallback(async (modelData: CreateModelRequest) => {
    const newModel = await apiClient.createModel(modelData)
    setModels(prev => [newModel, ...prev])
    return newModel
  }, [])

  // 更新模型
  const updateModel = useCallback(async (id: number, modelData: UpdateModelRequest) => {
    const updatedModel = await apiClient.updateModel(id, modelData)
    setModels(prev => prev.map(model => 
      model.id === id ? updatedModel : model
    ))
    return updatedModel
  }, [])

  // 删除模型
  const deleteModel = useCallback(async (id: number) => {
    await apiClient.deleteModel(id)
    setModels(prev => prev.filter(model => model.id !== id))
  }, [])

  // 发布模型
  const publishModel = useCallback(async (id: number) => {
    const updatedModel = await apiClient.publishModel(id)
    setModels(prev => prev.map(model => 
      model.id === id ? updatedModel : model
    ))
    await fetchPublishedModels()
    return updatedModel
  }, [fetchPublishedModels])

  // 取消发布模型
  const unpublishModel = useCallback(async (id: number) => {
    const updatedModel = await apiClient.unpublishModel(id)
    setModels(prev => prev.map(model => 
      model.id === id ? updatedModel : model
    ))
    await fetchPublishedModels()
    return updatedModel
  }, [fetchPublishedModels])

  // 重新获取数据
  const refetch = useCallback(async () => {
    await Promise.all([
      fetchModels(),
      fetchPublishedModels()
    ])
  }, [fetchModels, fetchPublishedModels])

  // 初始化数据
  useEffect(() => {
    refetch()
  }, [refetch])

  return {
    models,
    publishedModels,
    loading,
    error,
    searchModels,
    getModelsByCapability,
    createModel,
    updateModel,
    deleteModel,
    publishModel,
    unpublishModel,
    refetch
  }
}

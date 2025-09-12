import { useState, useEffect, useCallback } from 'react'
import { apiClient } from '@/lib/api'
import type { Model, ModelStats, CreateModelRequest, UpdateModelRequest } from '@/types/model'

export function useModels() {
  const [models, setModels] = useState<Model[]>([])
  const [publishedModels, setPublishedModels] = useState<Model[]>([])
  const [stats, setStats] = useState<ModelStats | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  // 获取所有模型
  const fetchModels = useCallback(async () => {
    try {
      setLoading(true)
      setError(null)
      const data = await apiClient.getModels()
      setModels(data)
    } catch (err) {
      setError(err instanceof Error ? err.message : '获取模型列表失败')
    } finally {
      setLoading(false)
    }
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

  // 获取统计信息
  const fetchStats = useCallback(async () => {
    try {
      const data = await apiClient.getModelStats()
      setStats(data)
    } catch (err) {
      console.error('获取模型统计信息失败:', err)
    }
  }, [])

  // 搜索模型
  const searchModels = useCallback(async (keyword: string) => {
    try {
      setLoading(true)
      setError(null)
      const data = await apiClient.searchModels(keyword)
      setModels(data)
    } catch (err) {
      setError(err instanceof Error ? err.message : '搜索模型失败')
    } finally {
      setLoading(false)
    }
  }, [])

  // 按组织筛选
  const getModelsByOrganization = useCallback(async (organization: string) => {
    try {
      setLoading(true)
      setError(null)
      const data = await apiClient.getModelsByOrganization(organization)
      setModels(data)
    } catch (err) {
      setError(err instanceof Error ? err.message : '按组织筛选模型失败')
    } finally {
      setLoading(false)
    }
  }, [])

  // 按能力筛选
  const getModelsByCapability = useCallback(async (capability: string) => {
    try {
      setLoading(true)
      setError(null)
      const data = await apiClient.getModelsByCapability(capability)
      setModels(data)
    } catch (err) {
      setError(err instanceof Error ? err.message : '按能力筛选模型失败')
    } finally {
      setLoading(false)
    }
  }, [])

  // 创建模型
  const createModel = useCallback(async (modelData: CreateModelRequest) => {
    try {
      const newModel = await apiClient.createModel(modelData)
      setModels(prev => [newModel, ...prev])
      await fetchStats() // 更新统计信息
      return newModel
    } catch (err) {
      throw err
    }
  }, [fetchStats])

  // 更新模型
  const updateModel = useCallback(async (id: number, modelData: UpdateModelRequest) => {
    try {
      const updatedModel = await apiClient.updateModel(id, modelData)
      setModels(prev => prev.map(model => 
        model.id === id ? updatedModel : model
      ))
      await fetchStats() // 更新统计信息
      return updatedModel
    } catch (err) {
      throw err
    }
  }, [fetchStats])

  // 删除模型
  const deleteModel = useCallback(async (id: number) => {
    try {
      await apiClient.deleteModel(id)
      setModels(prev => prev.filter(model => model.id !== id))
      await fetchStats() // 更新统计信息
    } catch (err) {
      throw err
    }
  }, [fetchStats])

  // 发布模型
  const publishModel = useCallback(async (id: number) => {
    try {
      const updatedModel = await apiClient.publishModel(id)
      setModels(prev => prev.map(model => 
        model.id === id ? updatedModel : model
      ))
      await fetchPublishedModels()
      await fetchStats()
      return updatedModel
    } catch (err) {
      throw err
    }
  }, [fetchPublishedModels, fetchStats])

  // 取消发布模型
  const unpublishModel = useCallback(async (id: number) => {
    try {
      const updatedModel = await apiClient.unpublishModel(id)
      setModels(prev => prev.map(model => 
        model.id === id ? updatedModel : model
      ))
      await fetchPublishedModels()
      await fetchStats()
      return updatedModel
    } catch (err) {
      throw err
    }
  }, [fetchPublishedModels, fetchStats])

  // 重新获取数据
  const refetch = useCallback(async () => {
    await Promise.all([
      fetchModels(),
      fetchPublishedModels(),
      fetchStats()
    ])
  }, [fetchModels, fetchPublishedModels, fetchStats])

  // 初始化数据
  useEffect(() => {
    refetch()
  }, [refetch])

  return {
    models,
    publishedModels,
    stats,
    loading,
    error,
    searchModels,
    getModelsByOrganization,
    getModelsByCapability,
    createModel,
    updateModel,
    deleteModel,
    publishModel,
    unpublishModel,
    refetch
  }
}

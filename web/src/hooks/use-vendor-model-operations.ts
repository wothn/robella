'use client'

import { useState } from 'react'
import { CreateVendorModelRequest, UpdateVendorModelRequest } from '@/types'
import { apiClient } from '@/lib/api'

interface UseVendorModelOperationsProps {
  onVendorModelsChange: () => void
}

export function useVendorModelOperations({ onVendorModelsChange }: UseVendorModelOperationsProps) {
  const [deleteModelId, setDeleteModelId] = useState<number | null>(null)
  const [isDeleting, setIsDeleting] = useState(false)
  const [isUpdating, setIsUpdating] = useState(false)
  const [isCreating, setIsCreating] = useState(false)

  const handleCreateVendorModel = async (data: CreateVendorModelRequest) => {
    setIsCreating(true)
    try {
      await apiClient.createVendorModel(data.providerId, data)
      onVendorModelsChange()
    } catch (error) {
      console.error('Failed to create vendor model:', error)
      throw error
    } finally {
      setIsCreating(false)
    }
  }

  const handleUpdateVendorModel = async (modelId: number, data: UpdateVendorModelRequest) => {
    setIsUpdating(true)
    try {
      await apiClient.updateVendorModel(modelId, data)
      onVendorModelsChange()
    } catch (error) {
      console.error('Failed to update vendor model:', error)
      throw error
    } finally {
      setIsUpdating(false)
    }
  }

  const handleDeleteVendorModel = async () => {
    if (!deleteModelId) return

    setIsDeleting(true)
    try {
      await apiClient.deleteVendorModel(deleteModelId)
      onVendorModelsChange()
      setDeleteModelId(null)
    } catch (error) {
      console.error('Failed to delete vendor model:', error)
      throw error
    } finally {
      setIsDeleting(false)
    }
  }

  const handleCreateVendorModelWrapper = (data: CreateVendorModelRequest | UpdateVendorModelRequest) => {
    // When creating a new vendor model, we only expect CreateVendorModelRequest
    if (!('id' in data) || !data.id) {
      return handleCreateVendorModel(data as CreateVendorModelRequest)
    }
  }

  return {
    // State
    deleteModelId,
    setDeleteModelId,
    isDeleting,
    isUpdating,
    isCreating,

    // Actions
    handleCreateVendorModel: handleCreateVendorModelWrapper,
    handleUpdateVendorModel,
    handleDeleteVendorModel
  }
}
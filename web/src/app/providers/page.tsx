'use client'

import { useState } from 'react'
import { Provider } from '@/types'

import { PageHeader } from "@/components/layout/page-header"
import { Settings } from 'lucide-react'
import { useProviders } from '@/hooks/use-providers'
import { ErrorHandler } from '@/components/error-handler'
import { LoadingState } from '@/components/loading-state'
import { 
  ProviderList, 
  ProviderDetails, 
  ProviderFormDialog, 
  type ProviderFormData 
} from '@/components/providers'
import { Toaster } from '@/components/ui/sonner'

export default function ProvidersPage() {
  const {
    providers,
    selectedProvider,
    vendorModels,
    loading,
    error,
    setSelectedProvider,
    createProvider,
    updateProvider,
    deleteProvider,
    refreshVendorModels,
    clearError
  } = useProviders()

  const [isCreateDialogOpen, setIsCreateDialogOpen] = useState(false)
  const [editingProvider, setEditingProvider] = useState<Provider | null>(null)

  const handleCreateProvider = async (formData: ProviderFormData) => {
    try {
      await createProvider(formData)
      setIsCreateDialogOpen(false)
    } catch (error) {
      // Error is handled by the hook
    }
  }

  const handleUpdateProvider = async (formData: ProviderFormData) => {
    if (!editingProvider) return

    try {
      await updateProvider(editingProvider.id, formData)
      setIsCreateDialogOpen(false)
      setEditingProvider(null)
    } catch (error) {
      // Error is handled by the hook
    }
  }

  const handleDeleteProvider = async (providerId: number) => {
    if (!confirm('确定要删除这个Provider吗？')) return

    try {
      await deleteProvider(providerId)
    } catch (error) {
      // Error is handled by the hook
    }
  }

  const openEditDialog = (provider: Provider) => {
    setEditingProvider(provider)
    setIsCreateDialogOpen(true)
  }

  if (loading) {
    return (
      <>
        <PageHeader title="Providers" />
        <div className="flex flex-1 flex-col gap-4 p-4 pt-0">
          <LoadingState message="加载 Providers..." size="lg" className="h-64" />
        </div>
      </>
    )
  }

  return (
    <>
      <PageHeader title="Providers" />
      
      <div className="flex flex-1 flex-col gap-4 p-4 pt-0">
        <div className="flex h-[calc(100vh-8rem)]">
          <ProviderList
            providers={providers}
            selectedProvider={selectedProvider}
            onProviderSelect={setSelectedProvider}
            onCreateNew={() => {
              setEditingProvider(null)
              setIsCreateDialogOpen(true)
            }}
          />

          {selectedProvider ? (
            <ProviderDetails
              provider={selectedProvider}
              vendorModels={vendorModels}
              onEdit={openEditDialog}
              onDelete={handleDeleteProvider}
              onVendorModelsChange={() => refreshVendorModels(selectedProvider.id)}
            />
          ) : (
            <div className="flex-1 flex items-center justify-center">
              <div className="text-center">
                <Settings className="h-12 w-12 text-gray-400 mx-auto mb-4" />
                <h3 className="text-lg font-medium text-gray-900 mb-2">选择一个 Provider</h3>
                <p className="text-gray-500">
                  从左侧列表中选择一个 Provider 查看详细信息
                </p>
              </div>
            </div>
          )}
        </div>

        <ProviderFormDialog
          isOpen={isCreateDialogOpen}
          onOpenChange={(open) => {
            setIsCreateDialogOpen(open)
            if (!open) {
              setEditingProvider(null)
            }
          }}
          provider={editingProvider}
          onSubmit={editingProvider ? handleUpdateProvider : handleCreateProvider}
          title={editingProvider ? '编辑 Provider' : '创建新 Provider'}
          description={editingProvider ? '修改 Provider 的配置信息' : '添加一个新的 AI 服务提供商'}
        />

        <Toaster />
        <ErrorHandler error={error} onClearError={clearError} />
      </div>
    </>
  )
}
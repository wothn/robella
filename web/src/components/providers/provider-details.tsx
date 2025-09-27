'use client'

import { useState } from 'react'
import { Provider, VendorModel, CreateVendorModelRequest, UpdateVendorModelRequest } from '@/types'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { VendorModelModal } from '@/components/providers/vendor-model-modal'
import { apiClient } from '@/lib/api'
import { ScrollArea } from '@/components/ui/scroll-area'
import {
  ProviderBasicInfo,
  ProviderDetailsCard,
  ProviderConfigCard
} from './provider-basic-info'
import { VendorModelsList } from './vendor-models-list'
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog'

interface ProviderDetailsProps {
  provider: Provider
  vendorModels: VendorModel[]
  onEdit: (provider: Provider) => void
  onDelete: (providerId: number) => void
  onVendorModelsChange: () => void
}

export function ProviderDetails({
  provider,
  vendorModels,
  onEdit,
  onDelete,
  onVendorModelsChange
}: ProviderDetailsProps) {
  const [deleteModelId, setDeleteModelId] = useState<number | null>(null)
  
  const handleCreateVendorModel = async (data: CreateVendorModelRequest) => {
    try {
      await apiClient.createVendorModel(provider.id, data)
      onVendorModelsChange()
    } catch (error) {
      console.error('Failed to create vendor model:', error)
    }
  }

  const handleUpdateVendorModel = async (modelId: number, data: UpdateVendorModelRequest) => {
    try {
      await apiClient.updateVendorModel(modelId, data)
      onVendorModelsChange()
    } catch (error) {
      console.error('Failed to update vendor model:', error)
    }
  }

  const handleCreateVendorModelWrapper = (data: CreateVendorModelRequest | UpdateVendorModelRequest) => {
    // When creating a new vendor model, we only expect CreateVendorModelRequest
    if (!('id' in data) || !data.id) {
      handleCreateVendorModel(data as CreateVendorModelRequest);
    }
  };

  const handleDeleteVendorModel = async () => {
    if (!deleteModelId) return
    try {
      await apiClient.deleteVendorModel(deleteModelId)
      onVendorModelsChange()
      setDeleteModelId(null)
    } catch (error) {
      console.error('Failed to delete vendor model:', error)
    }
  }

  return (
    <div className="flex-1 overflow-hidden">
      <ScrollArea className="h-full">
        <div className="p-6">
          <ProviderBasicInfo
            provider={provider}
            onEdit={() => onEdit(provider)}
            onDelete={() => onDelete(provider.id)}
          />

          <Tabs defaultValue="details" className="w-full">
            <TabsList className="grid w-full grid-cols-2">
              <TabsTrigger value="details" className="text-sm font-medium">详细信息</TabsTrigger>
              <TabsTrigger value="models" className="text-sm font-medium">
                模型列表 ({vendorModels.length})
              </TabsTrigger>
            </TabsList>

            <TabsContent value="details" className="space-y-6 mt-6">
              <ProviderDetailsCard provider={provider} />
              <ProviderConfigCard provider={provider} />
            </TabsContent>

            <TabsContent value="models" className="space-y-6 mt-6">
              <div className="flex items-center justify-between">
                <h3 className="text-lg font-semibold">Vendor Models</h3>
                <VendorModelModal
                  providerId={provider.id}
                  onSubmit={handleCreateVendorModelWrapper}
                />
              </div>

              <VendorModelsList
                vendorModels={vendorModels}
                providerId={provider.id}
                onUpdateModel={handleUpdateVendorModel}
                onDeleteModel={setDeleteModelId}
              />
            </TabsContent>
          </Tabs>
        </div>
      </ScrollArea>

      {/* 删除确认对话框 */}
      <AlertDialog open={!!deleteModelId} onOpenChange={() => setDeleteModelId(null)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>确认删除</AlertDialogTitle>
            <AlertDialogDescription>
              此操作将永久删除该Vendor Model，且无法撤销。确定要继续吗？
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>取消</AlertDialogCancel>
            <AlertDialogAction onClick={handleDeleteVendorModel} className="bg-destructive hover:bg-destructive/90">
              删除
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  )
}
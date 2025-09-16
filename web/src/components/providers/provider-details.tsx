'use client'

import { Provider, VendorModel } from '@/types'
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
  const handleCreateVendorModel = async (data: any) => {
    try {
      await apiClient.createVendorModel(provider.id, data)
      onVendorModelsChange()
    } catch (error) {
      console.error('Failed to create vendor model:', error)
    }
  }

  const handleUpdateVendorModel = async (modelId: number, data: any) => {
    try {
      await apiClient.updateVendorModel(modelId, data)
      onVendorModelsChange()
    } catch (error) {
      console.error('Failed to update vendor model:', error)
    }
  }

  const handleDeleteVendorModel = async (modelId: number) => {
    if (!confirm('确定要删除这个Vendor Model吗？')) return
    try {
      await apiClient.deleteVendorModel(modelId)
      onVendorModelsChange()
    } catch (error) {
      console.error('Failed to delete vendor model:', error)
    }
  }

  return (
    <div className="flex-1 overflow-hidden">
      <ScrollArea className="h-full">
        <div className="p-6 pr-4">
          <ProviderBasicInfo
            provider={provider}
            onEdit={() => onEdit(provider)}
            onDelete={() => onDelete(provider.id)}
          />

          <Tabs defaultValue="details" className="w-full">
            <TabsList>
              <TabsTrigger value="details">详细信息</TabsTrigger>
              <TabsTrigger value="models">模型列表 ({vendorModels.length})</TabsTrigger>
            </TabsList>

            <TabsContent value="details" className="space-y-6">
              <ProviderDetailsCard provider={provider} />
              <ProviderConfigCard provider={provider} />
            </TabsContent>

            <TabsContent value="models" className="space-y-4">
              <div className="flex items-center justify-between">
                <h3 className="text-lg font-medium">Vendor Models</h3>
                <VendorModelModal
                  providerId={provider.id}
                  onSubmit={handleCreateVendorModel}
                />
              </div>

              <VendorModelsList
                vendorModels={vendorModels}
                providerId={provider.id}
                onUpdateModel={handleUpdateVendorModel}
                onDeleteModel={handleDeleteVendorModel}
              />
            </TabsContent>
          </Tabs>
        </div>
      </ScrollArea>
    </div>
  )
}
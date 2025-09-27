import { memo, useState } from 'react'
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
import { useModelsStore } from '@/stores/models-store'
import { toast } from 'sonner'

export const ModelDeleteDialog = memo(() => {
  const { deleteModelId, setDeleteModelId, deleteModelFunction } = useModelsStore()
  const [loading, setLoading] = useState(false)

  const handleDelete = async () => {
    if (!deleteModelId || !deleteModelFunction) return

    try {
      setLoading(true)
      await deleteModelFunction(deleteModelId)
      setDeleteModelId(null)
      toast.success('删除成功')
    } catch (error) {
      console.error('删除模型失败:', error)
      toast.error('删除失败，请重试')
    } finally {
      setLoading(false)
    }
  }

  return (
    <AlertDialog open={!!deleteModelId} onOpenChange={() => setDeleteModelId(null)}>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>确认删除</AlertDialogTitle>
          <AlertDialogDescription>
            此操作将永久删除该模型，且无法撤销。确定要继续吗？
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel>取消</AlertDialogCancel>
          <AlertDialogAction
            onClick={handleDelete}
            className="bg-red-600 hover:bg-red-700"
            disabled={loading}
          >
            {loading ? '删除中...' : '删除'}
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  )
})

ModelDeleteDialog.displayName = 'ModelDeleteDialog'
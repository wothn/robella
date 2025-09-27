import { useState, useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import * as z from 'zod'
import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import {
  Form,
  FormControl,
  FormDescription,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from '@/components/ui/form'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import { Switch } from '@/components/ui/switch'
import { Checkbox } from '@/components/ui/checkbox'
import { Label } from '@/components/ui/label'
import { ScrollArea } from '@/components/ui/scroll-area'
import { useModels } from '@/hooks/use-models'
import { MODEL_CAPABILITIES, CAPABILITY_NAMES } from '@/constants/model-constants'
import type { Model, ModelCapability, CreateModelRequest, UpdateModelRequest } from '@/types/model'

const formSchema = z.object({
  name: z.string().min(1, '模型名称不能为空').max(100, '模型名称不能超过100个字符'),
  modelKey: z.string().min(1, '模型调用标识不能为空').max(200, '模型调用标识不能超过200个字符'),
  description: z.string().optional(),
  organization: z.string().optional(),
  capabilities: z.array(z.string()).optional(),
  contextWindow: z.number().int().min(1).optional(),
  published: z.boolean(),
})

type FormData = z.infer<typeof formSchema>

interface ModelFormDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  model?: Model | null
  onSuccess: () => void
}

export function ModelFormDialog({ open, onOpenChange, model, onSuccess }: ModelFormDialogProps) {
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const { createModel, updateModel } = useModels()

  const form = useForm<FormData>({
    resolver: zodResolver(formSchema),
    defaultValues: {
      name: '',
      description: '',
      organization: '',
      capabilities: [],
      contextWindow: undefined,
      published: false,
    },
  })

  const isEdit = !!model

  // 重置表单数据
  useEffect(() => {
    if (open) {
      setError(null) // 清除之前的错误
      if (model) {
        form.reset({
          name: model.name,
          modelKey: model.modelKey,
          description: model.description || '',
          organization: model.organization || '',
          capabilities: model.capabilities || [],
          contextWindow: model.contextWindow,
          published: model.published,
        })
      } else {
        form.reset({
          name: '',
          modelKey: '',
          description: '',
          organization: '',
          capabilities: [],
          contextWindow: undefined,
          published: false,
        })
      }
    }
  }, [open, model, form])

  const handleSubmit = async (data: FormData) => {
    try {
      setLoading(true)
      setError(null)

      const requestData = {
        ...data,
        capabilities: data.capabilities as ModelCapability[],
      }

      if (isEdit && model) {
        await updateModel(model.id, requestData as UpdateModelRequest)
      } else {
        await createModel(requestData as CreateModelRequest)
      }

      onSuccess()
      onOpenChange(false)
    } catch (error) {
      console.error('保存模型失败:', error)
      setError(error instanceof Error ? error.message : '保存失败，请重试')
    } finally {
      setLoading(false)
    }
  }

  const handleCapabilityChange = (capability: ModelCapability, checked: boolean) => {
    const currentCapabilities = form.getValues('capabilities') || []
    let newCapabilities: string[]

    if (checked) {
      newCapabilities = [...currentCapabilities, capability]
    } else {
      newCapabilities = currentCapabilities.filter(c => c !== capability)
    }

    form.setValue('capabilities', newCapabilities, { shouldValidate: true })
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-[600px] max-h-[80vh] overflow-hidden flex flex-col">
        <DialogHeader>
          <DialogTitle>
            {isEdit ? '编辑模型' : '创建新模型'}
          </DialogTitle>
          <DialogDescription>
            {isEdit
              ? '修改模型的基本信息和配置。'
              : '填写模型的基本信息，创建后可以进一步配置。'
            }
          </DialogDescription>
        </DialogHeader>

        <ScrollArea className="h-[60vh]">
          <Form {...form}>
            <form onSubmit={form.handleSubmit(handleSubmit)} className="space-y-6 pr-4">
            {/* 错误信息显示 */}
            {error && (
              <div className="bg-red-50 border border-red-200 rounded-md p-3">
                <div className="text-sm text-red-800">{error}</div>
              </div>
            )}

            {/* 基本信息 */}
            <div className="space-y-4">
              <div className="text-sm font-medium">基本信息</div>

              <FormField
                control={form.control}
                name="name"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>模型名称 *</FormLabel>
                    <FormControl>
                      <Input placeholder="输入模型名称" {...field} />
                    </FormControl>
                    <FormDescription>
                      模型的显示名称，用于界面展示
                    </FormDescription>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="modelKey"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>模型调用标识 *</FormLabel>
                    <FormControl>
                      <Input placeholder="输入模型调用标识" {...field} />
                    </FormControl>
                    <FormDescription>
                      模型的唯一标识，用于API调用和路由
                    </FormDescription>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="organization"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>组织</FormLabel>
                    <FormControl>
                      <Input placeholder="输入组织名称" {...field} />
                    </FormControl>
                    <FormDescription>
                      模型的开发组织或公司
                    </FormDescription>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="description"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>描述</FormLabel>
                    <FormControl>
                      <Textarea
                        placeholder="输入模型描述..."
                        className="min-h-[100px]"
                        {...field}
                      />
                    </FormControl>
                    <FormDescription>
                      详细描述模型的特点和用途
                    </FormDescription>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </div>

            {/* 技术配置 */}
            <div className="space-y-4">
              <div className="text-sm font-medium">技术配置</div>

              <FormField
                control={form.control}
                name="contextWindow"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>上下文窗口</FormLabel>
                    <FormControl>
                      <Input
                        type="number"
                        placeholder="8192"
                        {...field}
                        onChange={(e) => field.onChange(e.target.value ? parseInt(e.target.value) : undefined)}
                        value={field.value || ''}
                      />
                    </FormControl>
                    <FormDescription>
                      模型支持的最大上下文长度（token数）
                    </FormDescription>
                    <FormMessage />
                  </FormItem>
                )}
              />

              {/* 能力选择 */}
              <FormField
                control={form.control}
                name="capabilities"
                render={() => (
                  <FormItem>
                    <FormLabel>模型能力</FormLabel>
                    <FormDescription>
                      选择模型支持的能力类型
                    </FormDescription>
                    <div className="grid grid-cols-2 gap-3 mt-2">
                      {MODEL_CAPABILITIES.map((capability) => {
                        const currentCapabilities = form.watch('capabilities') || []
                        const isChecked = currentCapabilities.includes(capability)

                        return (
                          <div key={capability} className="flex items-center space-x-2">
                            <Checkbox
                              id={capability}
                              checked={isChecked}
                              onCheckedChange={(checked) =>
                                handleCapabilityChange(capability, checked as boolean)
                              }
                            />
                            <Label htmlFor={capability} className="text-sm">
                              {CAPABILITY_NAMES[capability]}
                            </Label>
                          </div>
                        )
                      })}
                    </div>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </div>

            {/* 发布设置 */}
            <div className="space-y-4">
              <div className="text-sm font-medium">发布设置</div>

              <FormField
                control={form.control}
                name="published"
                render={({ field }) => (
                  <FormItem className="flex flex-row items-center justify-between rounded-lg border p-4">
                    <div className="space-y-0.5">
                      <FormLabel className="text-base">
                        立即发布
                      </FormLabel>
                      <FormDescription>
                        发布后模型将对用户可见
                      </FormDescription>
                    </div>
                    <FormControl>
                      <Switch
                        checked={field.value}
                        onCheckedChange={field.onChange}
                      />
                    </FormControl>
                  </FormItem>
                )}
              />
            </div>

            <DialogFooter>
              <Button
                type="button"
                variant="outline"
                onClick={() => onOpenChange(false)}
                disabled={loading}
              >
                取消
              </Button>
              <Button type="submit" disabled={loading}>
                {loading ? '保存中...' : (isEdit ? '更新' : '创建')}
              </Button>
            </DialogFooter>
          </form>
        </Form>
        </ScrollArea>
      </DialogContent>
    </Dialog>
  )
}

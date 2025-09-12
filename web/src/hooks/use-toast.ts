import { useState, useCallback } from 'react'

interface Toast {
  title: string
  description?: string
  variant?: 'default' | 'destructive'
}

interface ToastState {
  toasts: (Toast & { id: string })[]
}

let toastId = 0

export function useToast() {
  const [state, setState] = useState<ToastState>({ toasts: [] })

  const toast = useCallback(({ title, description, variant = 'default' }: Toast) => {
    const id = (toastId++).toString()
    const newToast = { id, title, description, variant }
    
    setState(prev => ({
      toasts: [...prev.toasts, newToast]
    }))

    // 简单的console输出，可以后续集成真正的toast组件
    console.log(`Toast [${variant}]: ${title}${description ? ` - ${description}` : ''}`)
    
    // 3秒后自动移除
    setTimeout(() => {
      setState(prev => ({
        toasts: prev.toasts.filter(t => t.id !== id)
      }))
    }, 3000)
  }, [])

  return { toast, toasts: state.toasts }
}
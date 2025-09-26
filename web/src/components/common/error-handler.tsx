'use client'

import { toast } from 'sonner'
import { useEffect } from 'react'

interface ErrorHandlerProps {
  error: string | null
  onClearError: () => void
}

export function ErrorHandler({ error, onClearError }: ErrorHandlerProps) {
  useEffect(() => {
    if (error) {
      toast.error(error)
      onClearError()
    }
  }, [error, onClearError])

  return null
}
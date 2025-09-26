import React from 'react'
import { cn } from '@/lib/utils'
import { useLoadingStore } from '@/stores/loading-store'

interface LoadingProps {
  variant?: 'spinner' | 'dots' | 'bars' | 'overlay'
  size?: 'sm' | 'md' | 'lg'
  className?: string
  text?: string
  showGlobalOnly?: boolean
}

export function Loading({
  variant = 'spinner',
  size = 'md',
  className,
  text,
  showGlobalOnly = false
}: LoadingProps) {
  const isLoading = useLoadingStore((state) => state.isLoading)
  const globalLoading = useLoadingStore((state) => state.global)

  // 如果只显示全局loading且全局不在loading中，则不显示
  if (showGlobalOnly && !globalLoading) {
    return null
  }

  // 如果没有任何loading状态，则不显示
  if (!isLoading && !showGlobalOnly) {
    return null
  }

  const sizeClasses = {
    sm: 'h-4 w-4',
    md: 'h-8 w-8',
    lg: 'h-12 w-12'
  }

  const baseClasses = cn(
    'inline-block animate-spin',
    sizeClasses[size],
    className
  )

  if (variant === 'overlay') {
    return (
      <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
        <div className="bg-white p-6 rounded-lg shadow-lg flex flex-col items-center">
          <Loading variant="spinner" size="lg" />
          {text && <p className="mt-4 text-gray-600">{text}</p>}
        </div>
      </div>
    )
  }

  if (variant === 'dots') {
    return (
      <div className={cn('flex space-x-1', className)}>
        <div className={cn('bg-current rounded-full animate-bounce', sizeClasses[size])} style={{ animationDelay: '0ms' }} />
        <div className={cn('bg-current rounded-full animate-bounce', sizeClasses[size])} style={{ animationDelay: '150ms' }} />
        <div className={cn('bg-current rounded-full animate-bounce', sizeClasses[size])} style={{ animationDelay: '300ms' }} />
      </div>
    )
  }

  if (variant === 'bars') {
    return (
      <div className={cn('flex space-x-1', className)}>
        <div className={cn('bg-current animate-pulse', sizeClasses[size])} style={{ animationDelay: '0ms' }} />
        <div className={cn('bg-current animate-pulse', sizeClasses[size])} style={{ animationDelay: '200ms' }} />
        <div className={cn('bg-current animate-pulse', sizeClasses[size])} style={{ animationDelay: '400ms' }} />
      </div>
    )
  }

  // Default spinner
  return (
    <div className="flex flex-col items-center">
      <svg
        className={baseClasses}
        xmlns="http://www.w3.org/2000/svg"
        fill="none"
        viewBox="0 0 24 24"
      >
        <circle
          className="opacity-25"
          cx="12"
          cy="12"
          r="10"
          stroke="currentColor"
          strokeWidth="4"
        ></circle>
        <path
          className="opacity-75"
          fill="currentColor"
          d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
        ></path>
      </svg>
      {text && <p className="mt-2 text-sm text-gray-600">{text}</p>}
    </div>
  )
}

// Page loading wrapper
interface PageLoadingProps {
  page: string
  children: React.ReactNode
  fallback?: React.ReactNode
}

export function PageLoading({ page, children, fallback }: PageLoadingProps) {
  const loading = useLoadingStore((state) => state.getPageLoading(page))

  if (loading) {
    return fallback || (
      <div className="flex items-center justify-center p-8">
        <Loading text="加载中..." />
      </div>
    )
  }

  return <>{children}</>
}

// Component loading wrapper
interface ComponentLoadingProps {
  component: string
  children: React.ReactNode
  fallback?: React.ReactNode
}

export function ComponentLoading({ component, children, fallback }: ComponentLoadingProps) {
  const loading = useLoadingStore((state) => state.getComponentLoading(component))

  if (loading) {
    return fallback || (
      <div className="flex items-center justify-center p-4">
        <Loading size="sm" />
      </div>
    )
  }

  return <>{children}</>
}

// Button with loading state
interface LoadingButtonProps {
  loading: boolean
  children: React.ReactNode
  disabled?: boolean
  className?: string
  onClick?: () => void
  type?: 'button' | 'submit' | 'reset'
  variant?: 'default' | 'destructive' | 'outline' | 'secondary' | 'ghost' | 'link'
}

export function LoadingButton({
  loading,
  children,
  disabled,
  className,
  onClick,
  type = 'button',
  variant = 'default'
}: LoadingButtonProps) {
  const baseClasses = 'relative inline-flex items-center px-4 py-2 border text-sm font-medium rounded-md disabled:opacity-50 disabled:cursor-not-allowed'

  const variantClasses = {
    default: 'bg-indigo-600 text-white border-transparent hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500',
    destructive: 'bg-red-600 text-white border-transparent hover:bg-red-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-red-500',
    outline: 'bg-white text-gray-700 border-gray-300 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500',
    secondary: 'bg-gray-600 text-white border-transparent hover:bg-gray-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-gray-500',
    ghost: 'bg-transparent text-gray-700 border-transparent hover:bg-gray-100 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-gray-500',
    link: 'bg-transparent text-blue-600 border-transparent hover:bg-transparent hover:text-blue-800 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500'
  }

  return (
    <button
      type={type}
      className={cn(
        baseClasses,
        variantClasses[variant],
        className
      )}
      disabled={disabled || loading}
      onClick={onClick}
    >
      {loading && (
        <Loading
          variant="spinner"
          size="sm"
          className="absolute left-3"
        />
      )}
      <span className={loading ? 'ml-6' : ''}>{children}</span>
    </button>
  )
}
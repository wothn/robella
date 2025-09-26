import { Loader2 } from 'lucide-react'

interface LoadingSpinnerProps {
  size?: 'sm' | 'md' | 'lg'
  className?: string
}

export function LoadingSpinner({ size = 'md', className = '' }: LoadingSpinnerProps) {
  const sizeClasses = {
    sm: 'h-4 w-4',
    md: 'h-8 w-8',
    lg: 'h-12 w-12'
  }

  return (
    <Loader2 className={`animate-spin ${sizeClasses[size]} ${className}`} />
  )
}

interface LoadingStateProps {
  message?: string
  size?: 'sm' | 'md' | 'lg'
  className?: string
}

export function LoadingState({ 
  message = 'Loading...', 
  size = 'md', 
  className = '' 
}: LoadingStateProps) {
  return (
    <div className={`flex items-center justify-center ${className}`}>
      <div className="text-center">
        <LoadingSpinner size={size} className="mx-auto mb-2" />
        <p className="text-gray-500 text-sm">{message}</p>
      </div>
    </div>
  )
}
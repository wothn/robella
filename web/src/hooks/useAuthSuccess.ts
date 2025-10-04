import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuthStore } from '@/stores/auth-store'
import { apiClient } from '@/lib/api'

export const useAuthSuccess = () => {
  const navigate = useNavigate()
  const { updateUser } = useAuthStore()
  const hasProcessed = useRef(false)
  const [isProcessing, setIsProcessing] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    // Skip if already processed (handles React StrictMode double execution)
    if (hasProcessed.current) {
      console.log('Auth success - Already processed, skipping')
      return
    }

    const handleAuthSuccess = async () => {
      hasProcessed.current = true
      setIsProcessing(true)
      setError(null)

      try {
        console.log('Auth success - Starting authentication process')

        // Get current user using existing session
        try {
          console.log('Auth success - Getting current user with existing session')
          const currentUser = await apiClient.getCurrentUser()
          updateUser(currentUser)
          console.log('Auth success - User loaded, redirecting in 3 seconds')

          // 3秒后跳转到主页
          setTimeout(() => {
            navigate('/', { replace: true })
          }, 3000)
        } catch {
          throw new Error('Failed to get current user')
        }
      } catch (err) {
        console.error('Auth success failed:', err)
        setError(err instanceof Error ? err.message : 'Authentication failed')
        localStorage.removeItem('accessToken')
        navigate('/login', { replace: true })
      } finally {
        setIsProcessing(false)
      }
    }

    handleAuthSuccess()
  }, [navigate, updateUser])

  return { isProcessing, error }
}
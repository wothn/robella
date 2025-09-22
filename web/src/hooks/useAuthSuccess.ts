import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '@/contexts/AuthContext'
import { apiClient } from '@/lib/api'
import { storage } from '@/lib/storage'

export const useAuthSuccess = () => {
  const navigate = useNavigate()
  const { updateUser } = useAuth()
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

        // Try to refresh token using cookie
        try {
          console.log('Auth success - Attempting token refresh using cookie')
          const refreshResponse = await apiClient.refreshToken()
          console.log('Auth success - Token refresh successful')

          const currentUser = await apiClient.getCurrentUser()
          updateUser(currentUser)
          console.log('Auth success - User loaded, redirecting in 3 seconds')

          // 3秒后跳转到主页
          setTimeout(() => {
            navigate('/', { replace: true })
          }, 3000)
        } catch (refreshErr) {
          throw new Error('Failed to refresh token using cookie')
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
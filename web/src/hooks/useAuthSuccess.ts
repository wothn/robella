import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '@/contexts/AuthContext'
import { apiClient } from '@/lib/api'

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
        
        const params = new URLSearchParams(window.location.search)
        const token = params.get('token')
        const refreshToken = params.get('refreshToken')
        
        console.log('Auth success - URL params:', {
          hasToken: !!token,
          hasRefreshToken: !!refreshToken,
          pathname: window.location.pathname
        })

        if (token && refreshToken) {
          // New authentication
          localStorage.setItem('accessToken', token)
          localStorage.setItem('refreshToken', refreshToken)
          console.log('Auth success - Tokens stored')
          
          const currentUser = await apiClient.getCurrentUser()
          updateUser(currentUser)
          console.log('Auth success - User loaded, redirecting')
          
          navigate('/', { replace: true })
        } else {
          // Check existing session
          const existingToken = localStorage.getItem('accessToken')
          const existingRefreshToken = localStorage.getItem('refreshToken')
          
          if (existingToken && existingRefreshToken) {
            console.log('Auth success - Validating existing session')
            const currentUser = await apiClient.getCurrentUser()
            updateUser(currentUser)
            console.log('Auth success - Session valid, redirecting')
            navigate('/', { replace: true })
          } else {
            throw new Error('No authentication tokens found')
          }
        }
      } catch (err) {
        console.error('Auth success failed:', err)
        setError(err instanceof Error ? err.message : 'Authentication failed')
        localStorage.removeItem('accessToken')
        localStorage.removeItem('refreshToken')
        navigate('/login', { replace: true })
      } finally {
        setIsProcessing(false)
      }
    }

    handleAuthSuccess()
  }, [navigate, updateUser])

  return { isProcessing, error }
}
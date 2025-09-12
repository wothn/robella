import { useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '@/contexts/AuthContext'
import { apiClient } from '@/lib/api'

export default function AuthSuccessPage() {
  const navigate = useNavigate()
  const { updateUser } = useAuth()

  useEffect(() => {
    const handleSuccess = async () => {
      try {
        const params = new URLSearchParams(window.location.search)
        const token = params.get('token')
        const refreshToken = params.get('refreshToken')

        // 添加详细的调试日志
        console.log('Auth success page - Current URL:', window.location.href)
        console.log('Auth success page - URL search params:', window.location.search)
        console.log('Auth success page - Parsed token:', token ? 'present' : 'missing')
        console.log('Auth success page - Parsed refreshToken:', refreshToken ? 'present' : 'missing')

        if (token && refreshToken) {
          // Store tokens
          localStorage.setItem('accessToken', token)
          localStorage.setItem('refreshToken', refreshToken)
          
          console.log('Auth success page - Tokens stored successfully')
          
          // Get current user data
          const currentUser = await apiClient.getCurrentUser()
          updateUser(currentUser)
          
          console.log('Auth success page - User data loaded, redirecting to dashboard')
          
          // Redirect to dashboard
          navigate('/')
        } else {
          console.error('Missing token or refreshToken in success callback')
          console.error('Available URL parameters:', Array.from(params.keys()))
          navigate('/login')
        }
      } catch (error) {
        console.error('Failed to handle auth success:', error)
        navigate('/login')
      }
    }

    handleSuccess()
  }, [navigate, updateUser])

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50">
      <div className="text-center">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto"></div>
        <p className="mt-4 text-gray-600">正在完成登录...</p>
      </div>
    </div>
  )
}
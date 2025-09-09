import { useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '@/contexts/AuthContext'

export default function AuthSuccessPage() {
  const navigate = useNavigate()
  const { updateUser } = useAuth()

  useEffect(() => {
    const params = new URLSearchParams(window.location.search)
    const token = params.get('token')
    const user = params.get('user')

    if (token && user) {
      try {
        // 存储token和用户信息
        localStorage.setItem('accessToken', token)
        localStorage.setItem('user', user)
        
        // 解析用户信息
        const userData = JSON.parse(user)
        updateUser(userData)
        
        // 跳转到首页
        navigate('/')
      } catch (error) {
        console.error('Failed to parse user data:', error)
        navigate('/login')
      }
    } else {
      navigate('/login')
    }
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
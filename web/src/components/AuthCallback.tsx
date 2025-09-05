import React, { useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { CheckCircleIcon, XCircleIcon } from 'lucide-react'

interface AuthCallbackProps {
  type: 'success' | 'error'
}

const AuthCallback: React.FC<AuthCallbackProps> = ({ type }) => {
  const navigate = useNavigate()

  useEffect(() => {
    const urlParams = new URLSearchParams(window.location.search)
    const token = urlParams.get('token')
    const user = urlParams.get('user')

    if (type === 'success' && token && user) {
      // 存储用户信息
      localStorage.setItem('user', JSON.stringify({ username: user }))
      localStorage.setItem('accessToken', token)
      
      // 延迟跳转到主页面
      const timer = setTimeout(() => {
        navigate('/')
        window.location.reload() // 强制刷新以触发登录状态更新
      }, 2000)
      
      return () => clearTimeout(timer)
    }
  }, [type, navigate])

  if (type === 'success') {
    return (
      <div className="min-h-screen w-screen flex items-center justify-center bg-gradient-to-br from-green-50 via-blue-50 to-indigo-50 p-4">
        <Card className="w-full max-w-md backdrop-blur-xl bg-white/80 border-white/50 shadow-xl">
          <CardHeader className="text-center">
            <div className="flex justify-center mb-4">
              <CheckCircleIcon className="h-16 w-16 text-green-500" />
            </div>
            <CardTitle className="text-2xl font-bold text-green-600">登录成功！</CardTitle>
          </CardHeader>
          <CardContent className="text-center">
            <p className="text-gray-600 mb-4">正在跳转到主页面...</p>
            <div className="flex justify-center">
              <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-green-500"></div>
            </div>
          </CardContent>
        </Card>
      </div>
    )
  }

  if (type === 'error') {
    const urlParams = new URLSearchParams(window.location.search)
    const errorMessage = urlParams.get('error') || '登录失败'

    return (
      <div className="min-h-screen w-screen flex items-center justify-center bg-gradient-to-br from-red-50 via-pink-50 to-rose-50 p-4">
        <Card className="w-full max-w-md backdrop-blur-xl bg-white/80 border-white/50 shadow-xl">
          <CardHeader className="text-center">
            <div className="flex justify-center mb-4">
              <XCircleIcon className="h-16 w-16 text-red-500" />
            </div>
            <CardTitle className="text-2xl font-bold text-red-600">登录失败</CardTitle>
          </CardHeader>
          <CardContent className="text-center">
            <p className="text-gray-600 mb-6">{decodeURIComponent(errorMessage)}</p>
            <button
              onClick={() => navigate('/')}
              className="bg-blue-500 hover:bg-blue-600 text-white font-medium py-2 px-6 rounded-lg transition-colors"
            >
              返回登录页面
            </button>
          </CardContent>
        </Card>
      </div>
    )
  }

  return null
}

export default AuthCallback
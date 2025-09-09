import { useEffect } from 'react'
import { useNavigate } from 'react-router-dom'

export default function AuthErrorPage() {
  const navigate = useNavigate()

  useEffect(() => {
    const timer = setTimeout(() => {
      navigate('/login')
    }, 3000)

    return () => clearTimeout(timer)
  }, [navigate])

  const params = new URLSearchParams(window.location.search)
  const errorMessage = params.get('message') || '登录失败，请重试'

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50">
      <div className="text-center">
        <div className="text-red-600 text-6xl mb-4">⚠️</div>
        <h1 className="text-2xl font-bold text-gray-900 mb-2">登录失败</h1>
        <p className="text-gray-600 mb-4">{errorMessage}</p>
        <p className="text-sm text-gray-500">将在3秒后自动跳转到登录页面...</p>
      </div>
    </div>
  )
}
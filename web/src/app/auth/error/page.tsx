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
  const errorMessage = params.get('error') || '登录失败，请重试'

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-red-50 to-orange-50">
      <div className="bg-white rounded-2xl shadow-xl p-8 max-w-md mx-4 transform transition-all hover:scale-105">
        <div className="text-center">
          {/* Error Icon */}
          <div className="w-20 h-20 bg-red-100 rounded-full flex items-center justify-center mx-auto mb-6">
            <svg className="w-10 h-10 text-red-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
          </div>
          
          {/* Main content */}
          <h2 className="text-2xl font-bold text-gray-900 mb-3">登录失败</h2>
          <p className="text-red-600 bg-red-50 rounded-lg p-3 mb-4">{errorMessage}</p>
          
          {/* Auto redirect indicator */}
          <div className="space-y-2">
            <p className="text-gray-600 flex items-center justify-center">
              <svg className="w-4 h-4 mr-2 animate-pulse" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 7l5 5m0 0l-5 5m5-5H6" />
              </svg>
              正在跳转到登录页...
            </p>
            <div className="w-full bg-gray-200 rounded-full h-2">
              <div className="bg-red-600 h-2 rounded-full animate-pulse"></div>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
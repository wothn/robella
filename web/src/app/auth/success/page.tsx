import { useAuthSuccess } from '@/hooks/useAuthSuccess'

export default function AuthSuccessPage() {
  const { isProcessing, error } = useAuthSuccess()

  if (error) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-red-50 to-orange-50">
        <div className="bg-white rounded-2xl shadow-xl p-8 max-w-md mx-4 transform transition-all hover:scale-105">
          <div className="text-center">
            <div className="w-20 h-20 bg-red-100 rounded-full flex items-center justify-center mx-auto mb-6">
              <svg className="w-10 h-10 text-red-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
              </svg>
            </div>
            <h2 className="text-2xl font-bold text-gray-900 mb-3">认证失败</h2>
            <p className="text-red-600 bg-red-50 rounded-lg p-3 mb-4">{error}</p>
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

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-blue-50 via-indigo-50 to-purple-50">
      <div className="bg-white rounded-3xl shadow-2xl p-12 max-w-lg mx-4 transform transition-all hover:scale-105">
        <div className="text-center">
          {/* Animated Logo/Icon */}
          <div className="relative mb-8">
            <div className="w-24 h-24 bg-gradient-to-br from-blue-500 to-purple-600 rounded-full flex items-center justify-center mx-auto mb-6 shadow-lg">
              {isProcessing ? (
                <div className="w-12 h-12 border-4 border-white border-t-transparent rounded-full animate-spin"></div>
              ) : (
                <svg className="w-12 h-12 text-white animate-pulse" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                </svg>
              )}
            </div>
            
            {/* Decorative elements */}
            <div className="absolute -top-2 -right-2 w-6 h-6 bg-yellow-400 rounded-full animate-bounce"></div>
            <div className="absolute -bottom-2 -left-2 w-4 h-4 bg-pink-400 rounded-full animate-bounce" style={{ animationDelay: '0.2s' }}></div>
            <div className="absolute top-4 -right-4 w-3 h-3 bg-green-400 rounded-full animate-bounce" style={{ animationDelay: '0.4s' }}></div>
          </div>

          {/* Main content */}
          <h2 className="text-3xl font-bold text-gray-900 mb-4">
            {isProcessing ? '正在验证身份' : '登录成功！'}
          </h2>
          
          <p className="text-lg text-gray-600 mb-6">
            {isProcessing ? '请稍候，我们正在完成您的登录流程...' : '欢迎回来！正在为您跳转到主页...'}
          </p>

          {/* Progress indicators */}
          <div className="space-y-4 mb-8">
            <div className="flex justify-center space-x-2">
              <div className={`w-3 h-3 rounded-full transition-all duration-500 ${
                isProcessing ? 'bg-blue-500 animate-pulse' : 'bg-green-500'
              }`}></div>
              <div className={`w-3 h-3 rounded-full transition-all duration-500 delay-150 ${
                isProcessing ? 'bg-gray-300' : 'bg-blue-500 animate-pulse'
              }`}></div>
              <div className={`w-3 h-3 rounded-full transition-all duration-500 delay-300 ${
                isProcessing ? 'bg-gray-300' : 'bg-gray-300'
              }`}></div>
            </div>
            
            <div className="w-full bg-gray-200 rounded-full h-3 overflow-hidden">
              <div 
                className={`h-full rounded-full transition-all duration-1000 ${
                  isProcessing 
                    ? 'bg-gradient-to-r from-blue-500 to-purple-600 animate-pulse w-3/4' 
                    : 'bg-gradient-to-r from-green-500 to-blue-500 w-full'
                }`}
              ></div>
            </div>
          </div>

          {/* Feature highlights */}
          <div className="grid grid-cols-3 gap-4 text-center">
            <div className="bg-blue-50 rounded-lg p-3">
              <svg className="w-6 h-6 text-blue-600 mx-auto mb-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
              </svg>
              <p className="text-xs text-gray-600">安全认证</p>
            </div>
            <div className="bg-purple-50 rounded-lg p-3">
              <svg className="w-6 h-6 text-purple-600 mx-auto mb-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 10V3L4 14h7v7l9-11h-7z" />
              </svg>
              <p className="text-xs text-gray-600">快速响应</p>
            </div>
            <div className="bg-green-50 rounded-lg p-3">
              <svg className="w-6 h-6 text-green-600 mx-auto mb-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              <p className="text-xs text-gray-600">验证完成</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
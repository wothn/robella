import React, { useState, useEffect } from 'react'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { apiClient } from '@/lib/api'

import { LockIcon, UserIcon, EyeIcon, EyeOffIcon, GithubIcon } from 'lucide-react'

interface LoginFormData {
  username: string
  password: string
}

interface LoginProps {
  onLoginSuccess?: () => void
}

const Login: React.FC<LoginProps> = ({ onLoginSuccess }) => {
  const [formData, setFormData] = useState<LoginFormData>({
    username: '',
    password: ''
  })
  const [showPassword, setShowPassword] = useState(false)
  const [isLoading, setIsLoading] = useState(false)
  const [isGithubLoading, setIsGithubLoading] = useState(false)
  const [error, setError] = useState('')

  // 检查URL参数以处理OAuth回调
  useEffect(() => {
    const urlParams = new URLSearchParams(window.location.search)
    const token = urlParams.get('token')
    const user = urlParams.get('user')
    const errorMessage = urlParams.get('error')

    if (errorMessage) {
      setError(decodeURIComponent(errorMessage))
      // 清除URL中的错误参数
      window.history.replaceState({}, document.title, window.location.pathname)
      return
    }

    if (token && user) {
      // 存储用户信息
      localStorage.setItem('user', JSON.stringify({ username: user }))
      localStorage.setItem('accessToken', token)
      
      // 清除URL中的参数
      window.history.replaceState({}, document.title, window.location.pathname)
      
      // 触发登录成功回调
      if (onLoginSuccess) {
        onLoginSuccess()
      }
    }
  }, [onLoginSuccess])

  const handleGithubLogin = async () => {
    setIsGithubLoading(true)
    setError('')
    
    try {
      // 获取GitHub OAuth授权URL
      const redirectUri = `${window.location.origin}/api/auth/github/callback`
      const authUrl = `/api/auth/github/login?redirectUri=${encodeURIComponent(redirectUri)}`
      
      // 重定向到GitHub授权页面
      window.location.href = authUrl
    } catch (err: unknown) {
      console.error('GitHub登录错误:', err)
      if (err instanceof Error) {
        setError(err.message || 'GitHub登录失败，请稍后重试')
      } else {
        setError('GitHub登录失败，请稍后重试')
      }
      setIsGithubLoading(false)
    }
  }

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target
    setFormData(prev => ({
      ...prev,
      [name]: value
    }))
    // 清除错误信息
    if (error) setError('')
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    
    if (!formData.username || !formData.password) {
      setError('请输入用户名和密码')
      return
    }

    setIsLoading(true)
    setError('')

    try {
      console.log('Starting login process...')
      
      // 使用API客户端调用后端登录API
      const result = await apiClient.login(formData.username, formData.password)
      console.log('登录成功:', result)
      
      // 可以在这里处理登录响应，比如存储用户信息或token
      if (result.user) {
        localStorage.setItem('user', JSON.stringify(result.user))
      }
      if (result.accessToken) {
        localStorage.setItem('accessToken', result.accessToken)
      }
      
      // 调用父组件的登录成功回调
      if (onLoginSuccess) {
        onLoginSuccess()
      }
    } catch (err: unknown) {
      console.error('Login error:', err)
      
      if (err instanceof Error) {
        // 解析错误信息
        if (err.message.includes('status: 401')) {
          setError('用户名或密码错误')
        } else if (err.message.includes('status: 500')) {
          setError('服务器内部错误，请稍后重试')
        } else if (err.message.includes('Failed to fetch')) {
          setError('无法连接到服务器，请检查网络连接')
        } else {
          setError(err.message || '登录失败，请稍后重试')
        }
      } else {
        setError('网络错误，请稍后重试')
      }
    } finally {
      setIsLoading(false)
    }
  }

  return (
    <div className="min-h-screen w-screen flex items-center justify-center bg-gradient-to-br from-slate-50 via-blue-50 to-indigo-50 p-4 relative overflow-hidden">
      {/* 背景装饰元素 */}
      <div className="absolute inset-0 overflow-hidden">
        <div className="absolute -top-40 -right-40 w-80 h-80 bg-blue-200 rounded-full mix-blend-multiply filter blur-xl opacity-30 animate-blob"></div>
        <div className="absolute -bottom-40 -left-40 w-80 h-80 bg-indigo-200 rounded-full mix-blend-multiply filter blur-xl opacity-30 animate-blob animation-delay-2000"></div>
        <div className="absolute top-1/2 left-1/2 transform -translate-x-1/2 -translate-y-1/2 w-80 h-80 bg-purple-200 rounded-full mix-blend-multiply filter blur-xl opacity-30 animate-blob animation-delay-4000"></div>
      </div>

      {/* 网格背景 */}
      <div className="absolute inset-0 bg-grid-slate-200/[0.3] bg-[length:40px_40px]"></div>

      <Card className="w-full max-w-md backdrop-blur-xl bg-white/80 border-white/50 shadow-xl relative z-10">
        <CardHeader className="space-y-1 text-slate-800">
          <div className="flex items-center justify-center mb-6">
            <div className="relative">
              <div className="absolute inset-0 bg-white/60 backdrop-blur-sm rounded-full border border-slate-200 shadow-sm"></div>
              <LockIcon className="h-10 w-10 text-slate-700 relative z-10" />
            </div>
          </div>
          <CardTitle className="text-3xl font-bold text-center bg-gradient-to-r from-blue-600 to-indigo-600 bg-clip-text text-transparent">
            欢迎回来
          </CardTitle>
          <CardDescription className="text-center text-slate-600">
            请输入您的账户信息以继续
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-6">
          <form onSubmit={handleSubmit} className="space-y-5">
            <div className="space-y-2">
              <Label htmlFor="username" className="text-slate-700 font-medium">用户名</Label>
              <div className="relative">
                <UserIcon className="absolute left-4 top-1/2 transform -translate-y-1/2 h-5 w-5 text-slate-600" />
                <Input
                  id="username"
                  name="username"
                  type="text"
                  placeholder="请输入用户名"
                  value={formData.username}
                  onChange={handleInputChange}
                  className="pl-12 bg-white/60 border-slate-200 text-slate-800 placeholder:text-slate-400 focus:border-blue-400 focus:ring-blue-400/20 backdrop-blur-sm"
                  required
                />
              </div>
            </div>
            
            <div className="space-y-2">
              <Label htmlFor="password" className="text-slate-700 font-medium">密码</Label>
              <div className="relative">
                <LockIcon className="absolute left-4 top-1/2 transform -translate-y-1/2 h-5 w-5 text-slate-600" />
                <Input
                  id="password"
                  name="password"
                  type={showPassword ? "text" : "password"}
                  placeholder="请输入密码"
                  value={formData.password}
                  onChange={handleInputChange}
                  className="pl-12 pr-12 bg-white/60 border-slate-200 text-slate-800 placeholder:text-slate-400 focus:border-blue-400 focus:ring-blue-400/20 backdrop-blur-sm"
                  required
                />
                <Button
                  type="button"
                  variant="ghost"
                  size="sm"
                  className="absolute right-0 top-0 h-full px-4 py-2 hover:bg-transparent text-slate-500 hover:text-slate-700"
                  onClick={() => setShowPassword(!showPassword)}
                >
                  {showPassword ? (
                    <EyeOffIcon className="h-5 w-5" />
                  ) : (
                    <EyeIcon className="h-5 w-5" />
                  )}
                </Button>
              </div>
            </div>

            {error && (
              <div className="text-sm text-red-600 bg-red-50 border border-red-200 rounded-md p-3 backdrop-blur-sm">
                {error}
              </div>
            )}

            <Button 
              type="submit" 
              className="w-full bg-gradient-to-r from-blue-500 to-indigo-500 hover:from-blue-600 hover:to-indigo-600 text-white font-medium py-3 text-base shadow-lg hover:shadow-xl transition-all duration-200" 
              disabled={isLoading}
            >
              {isLoading ? (
                <div className="flex items-center justify-center">
                  <div className="animate-spin rounded-full h-5 w-5 border-b-2 border-white mr-2"></div>
                  登录中...
                </div>
              ) : '登录'}
            </Button>
          </form>

          <div className="relative">
            <div className="absolute inset-0 flex items-center">
              <div className="w-full border-t border-slate-200"></div>
            </div>
            <div className="relative flex justify-center text-sm">
              <span className="px-2 bg-white/60 backdrop-blur-sm text-slate-500">或者</span>
            </div>
          </div>

          <Button 
            type="button" 
            variant="outline" 
            className="w-full bg-white/60 border-slate-200 text-slate-700 hover:bg-slate-50 font-medium py-3 text-base shadow-sm hover:shadow-md transition-all duration-200 backdrop-blur-sm"
            onClick={handleGithubLogin}
            disabled={isGithubLoading}
          >
            {isGithubLoading ? (
              <div className="flex items-center justify-center">
                <div className="animate-spin rounded-full h-5 w-5 border-b-2 border-slate-600 mr-2"></div>
                GitHub登录中...
              </div>
            ) : (
              <div className="flex items-center justify-center">
                <GithubIcon className="h-5 w-5 mr-2" />
                使用GitHub登录
              </div>
            )}
          </Button>

          <div className="text-center text-sm text-slate-500">
            <p>忘记密码？ <a href="#" className="text-blue-600 hover:text-blue-700 hover:underline transition-colors">点击找回</a></p>
            <div className="mt-6 p-4 bg-gradient-to-r from-blue-50 to-indigo-50 border border-slate-200 rounded-md backdrop-blur-sm">
              <p className="text-slate-700 font-medium mb-2">演示账户</p>
              <div className="space-y-1 text-slate-600 text-xs">
                <p>用户名: admin | 密码: password123</p>
                <p>用户名: demo | 密码: password123</p>
              </div>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* 自定义动画样式 */}
      <style>{`
        @keyframes blob {
          0% {
            transform: translate(0px, 0px) scale(1);
          }
          33% {
            transform: translate(20px, -30px) scale(1.05);
          }
          66% {
            transform: translate(-15px, 15px) scale(0.95);
          }
          100% {
            transform: translate(0px, 0px) scale(1);
          }
        }
        .animate-blob {
          animation: blob 7s infinite;
        }
        .animation-delay-2000 {
          animation-delay: 2s;
        }
        .animation-delay-4000 {
          animation-delay: 4s;
        }
      `}</style>
    </div>
  )
}

export default Login

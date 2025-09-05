import { useState, useEffect } from 'react'
import { BrowserRouter as Router, Routes, Route, useLocation } from 'react-router-dom'
import Login from './components/Login'
import UserManagement from './components/UserManagement'
import AuthCallback from './components/AuthCallback'

function AppContent() {
  const [isLoggedIn, setIsLoggedIn] = useState(false)
  const location = useLocation()

  // 检查登录状态
  useEffect(() => {
    const user = localStorage.getItem('user')
    const accessToken = localStorage.getItem('accessToken')
    
    if (user && accessToken) {
      setIsLoggedIn(true)
    }
  }, [])

  // 处理OAuth回调
  useEffect(() => {
    const urlParams = new URLSearchParams(location.search)
    const auth = urlParams.get('auth')
    const token = urlParams.get('token')
    const user = urlParams.get('user')
    const errorMessage = urlParams.get('message')

    if (auth === 'error' && errorMessage) {
      // 可以在这里显示错误信息
      console.error('GitHub登录失败:', decodeURIComponent(errorMessage))
      // 清除URL参数
      window.history.replaceState({}, document.title, window.location.pathname)
      return
    }

    if (auth === 'success' && token && user) {
      localStorage.setItem('user', JSON.stringify({ username: user }))
      localStorage.setItem('accessToken', token)
      setIsLoggedIn(true)
      
      // 清除URL参数
      window.history.replaceState({}, document.title, window.location.pathname)
    }
  }, [location])

  const handleLoginSuccess = () => {
    setIsLoggedIn(true)
  }

  const handleLogout = () => {
    localStorage.removeItem('user')
    localStorage.removeItem('accessToken')
    setIsLoggedIn(false)
  }

  if (!isLoggedIn) {
    return <Login onLoginSuccess={handleLoginSuccess} />
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <header className="bg-white shadow">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between h-16">
            <div className="flex items-center">
              <h1 className="text-xl font-semibold">Robella 管理系统</h1>
            </div>
            <div className="flex items-center">
              <button
                onClick={handleLogout}
                className="text-gray-500 hover:text-gray-700 px-3 py-2 rounded-md text-sm font-medium"
              >
                退出登录
              </button>
            </div>
          </div>
        </div>
      </header>
      
      <main className="max-w-7xl mx-auto py-6 sm:px-6 lg:px-8">
        <div className="px-4 py-6 sm:px-0">
          <UserManagement />
        </div>
      </main>
    </div>
  )
}

function App() {
  return (
    <Router>
      <Routes>
        <Route path="/auth/success" element={<AuthCallback type="success" />} />
        <Route path="/auth/error" element={<AuthCallback type="error" />} />
        <Route path="*" element={<AppContent />} />
      </Routes>
    </Router>
  )
}

export default App

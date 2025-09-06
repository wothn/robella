import { useState, useEffect } from 'react'
import { BrowserRouter as Router, Routes, Route, useLocation } from 'react-router-dom'
import { Layout, Menu } from 'antd'
import { UserOutlined, ApiOutlined } from '@ant-design/icons'
import { AppProvider } from './contexts/AppContext'
import Login from './components/Login'
import UserManagement from './components/UserManagement'
import ProviderModelManager from './components/ProviderModelManager'
import AuthCallback from './components/AuthCallback'

function AppContent() {
  const [isLoggedIn, setIsLoggedIn] = useState(false)
  const [currentMenu, setCurrentMenu] = useState('users')
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

  const { Header, Content, Sider } = Layout

  const renderContent = () => {
    switch (currentMenu) {
      case 'users':
        return <UserManagement />
      case 'providers':
        return <ProviderModelManager />
      default:
        return <UserManagement />
    }
  }

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Header style={{ background: '#fff', padding: '0 24px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <h1 style={{ margin: 0, fontSize: '20px', fontWeight: 'bold' }}>Robella 管理系统</h1>
        <button
          onClick={handleLogout}
          style={{ 
            background: 'none', 
            border: 'none', 
            color: '#666', 
            cursor: 'pointer',
            padding: '8px 16px',
            borderRadius: '4px'
          }}
          onMouseEnter={(e) => e.currentTarget.style.color = '#333'}
          onMouseLeave={(e) => e.currentTarget.style.color = '#666'}
        >
          退出登录
        </button>
      </Header>
      
      <Layout>
        <Sider width={200} style={{ background: '#fff' }}>
          <Menu
            mode="inline"
            selectedKeys={[currentMenu]}
            style={{ height: '100%', borderRight: 0 }}
            onClick={(e) => setCurrentMenu(e.key)}
          >
            <Menu.Item key="users" icon={<UserOutlined />}>
              用户管理
            </Menu.Item>
            <Menu.Item key="providers" icon={<ApiOutlined />}>
              提供商与模型
            </Menu.Item>
          </Menu>
        </Sider>
        
        <Content style={{ padding: '24px', background: '#f0f2f5', margin: 0 }}>
          {renderContent()}
        </Content>
      </Layout>
    </Layout>
  )
}

function App() {
  return (
    <AppProvider>
      <Router>
        <Routes>
          <Route path="/auth/success" element={<AuthCallback type="success" />} />
          <Route path="/auth/error" element={<AuthCallback type="error" />} />
          <Route path="*" element={<AppContent />} />
        </Routes>
      </Router>
    </AppProvider>
  )
}

export default App

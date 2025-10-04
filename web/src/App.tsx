import { BrowserRouter as Router, Routes, Route } from 'react-router-dom'
import { useAuthStore } from './stores/auth-store'
import { useGlobalLoading } from './stores/loading-store'
import { Loading } from './components/common/loading'
import { DashboardLayout } from '@/components/layout'
import LoginPage from './app/login/page'
import RegisterPage from './app/register/page'
import DashboardPage from './app/dashboard/page'
import UsersPage from './app/users/page'
import ProvidersPage from './app/providers/page'
import ModelsPage from './app/models/page'
import ApiKeysPage from './app/apikeys/page'
import ProfilePage from './app/profile/page'
import AuthCallbackPage from './app/auth/callback/page'
import AuthSuccessPage from './app/auth/success/page'
import AuthErrorPage from './app/auth/error/page'
import { Navigate } from 'react-router-dom'

function AppContent() {
  const { user } = useAuthStore()
  const { loading } = useGlobalLoading()
  const isAuthenticated = !!user

  // 全局loading状态
  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50">
        <Loading text="加载中..." size="lg" />
      </div>
    )
  }

  return (
    <Routes>
      <Route 
        path="/" 
        element={isAuthenticated ? (
          <DashboardLayout>
            <DashboardPage />
          </DashboardLayout>
        ) : <Navigate to="/login" replace />} 
      />
      <Route 
        path="/dashboard" 
        element={isAuthenticated ? (
          <DashboardLayout>
            <DashboardPage />
          </DashboardLayout>
        ) : <Navigate to="/login" replace />} 
      />
      <Route 
        path="/login" 
        element={isAuthenticated ? <Navigate to="/" replace /> : <LoginPage />} 
      />
      <Route 
        path="/register" 
        element={isAuthenticated ? <Navigate to="/" replace /> : <RegisterPage />} 
      />
      <Route 
        path="/users" 
        element={isAuthenticated ? (
          <DashboardLayout>
            <UsersPage />
          </DashboardLayout>
        ) : <Navigate to="/login" replace />} 
      />
      <Route 
        path="/providers" 
        element={isAuthenticated ? (
          <DashboardLayout>
            <ProvidersPage />
          </DashboardLayout>
        ) : <Navigate to="/login" replace />} 
      />
      <Route
        path="/models"
        element={isAuthenticated ? (
          <DashboardLayout>
            <ModelsPage />
          </DashboardLayout>
        ) : <Navigate to="/login" replace />}
      />
      <Route
        path="/apikeys"
        element={isAuthenticated ? (
          <DashboardLayout>
            <ApiKeysPage />
          </DashboardLayout>
        ) : <Navigate to="/login" replace />}
      />
      <Route
        path="/profile"
        element={isAuthenticated ? (
          <DashboardLayout>
            <ProfilePage />
          </DashboardLayout>
        ) : <Navigate to="/login" replace />}
      />
      <Route path="/auth/callback" element={<AuthCallbackPage />} />
      <Route path="/auth/success" element={<AuthSuccessPage />} />
      <Route path="/auth/error" element={<AuthErrorPage />} />
    </Routes>
  )
}

function App() {
  return (
    <Router>
      <AppContent />
    </Router>
  )
}

export default App

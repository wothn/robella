import { BrowserRouter as Router, Routes, Route } from 'react-router-dom'
import { AppProvider } from './contexts/AppContext'
import { AuthProvider, useAuth } from './contexts/AuthContext'
import { DashboardLayout } from './components/layout/dashboard-layout'
import LoginPage from './app/login/page'
import DashboardPage from './app/dashboard/page'
import UsersPage from './app/users/page'
import ProvidersPage from './app/providers/page'
import ModelsPage from './app/models/page'
import AuthCallbackPage from './app/auth/callback/page'
import AuthSuccessPage from './app/auth/success/page'
import AuthErrorPage from './app/auth/error/page'
import { Navigate } from 'react-router-dom'

function AppContent() {
  const { isAuthenticated, loading } = useAuth()

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
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
      <Route path="/auth/callback" element={<AuthCallbackPage />} />
      <Route path="/auth/success" element={<AuthSuccessPage />} />
      <Route path="/auth/error" element={<AuthErrorPage />} />
    </Routes>
  )
}

function App() {
  return (
    <Router>
      <AuthProvider>
        <AppProvider>
          <AppContent />
        </AppProvider>
      </AuthProvider>
    </Router>
  )
}

export default App

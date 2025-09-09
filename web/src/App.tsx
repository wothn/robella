import { BrowserRouter as Router, Routes, Route } from 'react-router-dom'
import { AppProvider } from './contexts/AppContext'
import { AuthProvider, useAuth } from './contexts/AuthContext'
import LoginPage from './app/login/page'
import DashboardPage from './app/dashboard/page'
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
        element={isAuthenticated ? <DashboardPage /> : <Navigate to="/login" replace />} 
      />
      <Route 
        path="/login" 
        element={isAuthenticated ? <Navigate to="/" replace /> : <LoginPage />} 
      />
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

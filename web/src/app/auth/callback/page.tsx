import { useEffect, useState } from "react"
import { useNavigate } from "react-router-dom"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { useAuth } from "@/contexts/AuthContext"
import { apiClient } from "@/lib/api"
import { storage } from "@/lib/storage"
import type { LoginResponse } from "@/types/user"

export default function AuthCallbackPage() {
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const navigate = useNavigate()
  const { updateUser } = useAuth()

  useEffect(() => {
    const handleCallback = async () => {
      try {
        // Get URL parameters
        const urlParams = new URLSearchParams(window.location.search)
        const token = urlParams.get('token')
        const refreshToken = urlParams.get('refreshToken')

        if (token) {
          // Direct token from backend redirect (refreshToken is in HttpOnly cookie)
          localStorage.setItem('accessToken', token)

          // Get the current user data
          const currentUser = await apiClient.getCurrentUser()
          updateUser(currentUser)

          // Redirect to dashboard
          navigate('/')
        } else {
          // Fallback to code exchange flow
          const code = urlParams.get('code')
          const state = urlParams.get('state')

          if (!code || !state) {
            throw new Error('Missing required OAuth parameters')
          }

          // Exchange code for access token
          const response: LoginResponse = await apiClient.githubCallback(code, state)

          // Store the accessToken in localStorage, refreshToken is already in HttpOnly cookie
          localStorage.setItem('accessToken', response.accessToken)

          // Get the current user data
          const currentUser = await apiClient.getCurrentUser()
          updateUser(currentUser)

          // Redirect to dashboard
          navigate('/')
        }
      } catch (err) {
        console.error('OAuth callback error:', err)
        setError(err instanceof Error ? err.message : 'Authentication failed')
      } finally {
        setLoading(false)
      }
    }

    handleCallback()
  }, [navigate, updateUser])

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <Card className="w-[400px]">
          <CardHeader className="text-center">
            <CardTitle>Processing Login</CardTitle>
            <CardDescription>
              Please wait while we complete your authentication...
            </CardDescription>
          </CardHeader>
          <CardContent className="text-center">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary mx-auto"></div>
          </CardContent>
        </Card>
      </div>
    )
  }

  if (error) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <Card className="w-[400px]">
          <CardHeader className="text-center">
            <CardTitle className="text-destructive">Authentication Failed</CardTitle>
            <CardDescription>
              {error}
            </CardDescription>
          </CardHeader>
          <CardContent className="text-center">
            <button
              onClick={() => navigate('/login')}
              className="text-primary hover:underline"
            >
              Return to Login
            </button>
          </CardContent>
        </Card>
      </div>
    )
  }

  return null // This will redirect before showing anything
}
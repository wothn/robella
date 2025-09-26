import { useEffect, useState } from "react"
import { useNavigate } from "react-router-dom"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { useAuthStore } from "@/stores/auth-store"
import { useGlobalLoading } from "@/stores/loading-store"
import { Loading } from "@/components/common/loading"
import { apiClient } from "@/lib/api"

export default function AuthCallbackPage() {
  const [error, setError] = useState<string | null>(null)
  const navigate = useNavigate()
  const { updateUser } = useAuthStore()
  const { loading } = useGlobalLoading()

  useEffect(() => {
    const handleCallback = async () => {
      try {
        // Get URL parameters
        const urlParams = new URLSearchParams(window.location.search)
        const code = urlParams.get('code')
        const state = urlParams.get('state')

        if (!code || !state) {
          throw new Error('Missing required OAuth parameters')
        }

        // Exchange code for access token
        await apiClient.githubCallback(code, state)

        // Get the current user data
        const currentUser = await apiClient.getCurrentUser()
        updateUser(currentUser)

        // Redirect to dashboard
        navigate('/')
      } catch (err) {
        console.error('OAuth callback error:', err)
        setError(err instanceof Error ? err.message : 'Authentication failed')
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
            <Loading text="处理中..." size="md" />
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
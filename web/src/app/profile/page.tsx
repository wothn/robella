import React, { useCallback, useEffect, useMemo, useState } from "react"
import { useLocation, useNavigate } from "react-router-dom"
import { PageHeader } from "@/components/layout/page-header"
import { PageLoading } from "@/components/common/loading"
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Button } from "@/components/ui/button"
import { Separator } from "@/components/ui/separator"
import { Badge } from "@/components/ui/badge"
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar"
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import { LoadingSpinner } from "@/components/common/loading-state"
import { useAuthStore } from "@/stores/auth-store"
import { apiClient } from "@/lib/api"
import { withActionLoading, withPageLoading, useActionLoading } from "@/stores/loading-store"
import type { User, UserProfileUpdateRequest } from "@/types/user"
import { getRoleDisplayText } from "@/types/user"
import dayjs from "dayjs"
import { Github, ShieldCheck } from "lucide-react"

interface FeedbackState {
  type: "success" | "error"
  title: string
  description?: string
}

const defaultAvatar = "/avatars/default.jpg"

export default function ProfilePage() {
  const { user, updateUser } = useAuthStore()
  const navigate = useNavigate()
  const location = useLocation()
  const [profile, setProfile] = useState<User | null>(user)
  const [profileForm, setProfileForm] = useState({
    displayName: user?.displayName ?? ""
  })
  const [passwordForm, setPasswordForm] = useState({
    currentPassword: "",
    newPassword: "",
    confirmPassword: ""
  })
  const [loadError, setLoadError] = useState<string | null>(null)
  const [profileFeedback, setProfileFeedback] = useState<FeedbackState | null>(null)
  const [passwordFeedback, setPasswordFeedback] = useState<FeedbackState | null>(null)
  const [githubFeedback, setGithubFeedback] = useState<FeedbackState | null>(null)

  const profileAction = useActionLoading("profile-update")
  const passwordAction = useActionLoading("profile-password")
  const githubAction = useActionLoading("profile-github")

  const avatarFallback = useMemo(() => {
    if (!profileForm.displayName && !profile?.username) {
      return "U"
    }
    const base = profileForm.displayName || profile?.username || "User"
    return base.charAt(0).toUpperCase()
  }, [profileForm.displayName, profile?.username])

  const syncProfile = useCallback((current: User) => {
    setProfile(current)
    setProfileForm({
      displayName: current.displayName ?? ""
    })
    updateUser(current)
  }, [updateUser])

  const loadProfile = useCallback(async () => {
    setLoadError(null)
    setProfileFeedback(null)
    try {
      await withPageLoading("profile", async () => {
        try {
          const current = await apiClient.getCurrentUser()
          syncProfile(current)
        } catch (error) {
          console.error("Failed to load profile:", error)
          const message = error instanceof Error ? error.message : "未知错误"
          setLoadError(message)
          throw error
        }
      })
    } catch {
      // 错误已在内部处理，这里防止未捕获
    }
  }, [syncProfile])

  useEffect(() => {
    loadProfile().catch(() => {})
  }, [loadProfile])

  useEffect(() => {
    if (user) {
      syncProfile(user)
    } else {
      setProfile(null)
      setProfileForm({ displayName: "" })
    }
  }, [user, syncProfile])

  useEffect(() => {
    if (!location) return
    const searchParams = new URLSearchParams(location.search)
    const githubStatus = searchParams.get("github")
    if (!githubStatus) {
      return
    }

    if (githubStatus === "success") {
      setGithubFeedback({
        type: "success",
        title: "GitHub 绑定成功",
        description: "您现在可以使用 GitHub 账号登录。"
      })
      loadProfile().catch(() => {})
    } else if (githubStatus === "error") {
      setGithubFeedback({
        type: "error",
        title: "GitHub 绑定失败",
        description: "请稍后重试或联系管理员。"
      })
    }

    searchParams.delete("github")
    const nextSearch = searchParams.toString()
    navigate({ pathname: location.pathname, search: nextSearch ? `?${nextSearch}` : "" }, { replace: true })
  }, [location, loadProfile, navigate])

  const githubIdDisplay = useMemo(() => {
    if (!profile?.githubId) {
      return ""
    }
    const value = profile.githubId
    return value.length > 12 ? `${value.slice(0, 6)}...${value.slice(-4)}` : value
  }, [profile?.githubId])

  const handleGitHubBind = () => {
    setGithubFeedback(null)
    apiClient.githubBind()
  }

  const handleGitHubUnbind = async () => {
    setGithubFeedback(null)
    try {
      await withActionLoading("profile-github", async () => {
        await apiClient.unbindGithub()
        const current = await apiClient.getCurrentUser()
        syncProfile(current)
      })
      setGithubFeedback({
        type: "success",
        title: "已解除 GitHub 绑定",
        description: "您可以随时重新绑定新的 GitHub 账号。"
      })
    } catch (error) {
      console.error("Failed to unbind GitHub:", error)
      const message = error instanceof Error ? error.message : "请稍后重试"
      setGithubFeedback({
        type: "error",
        title: "解绑失败",
        description: message
      })
    }
  }

  const handleProfileSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setProfileFeedback(null)

    const payload: UserProfileUpdateRequest = {
      displayName: profileForm.displayName?.trim() || undefined
    }

    // Validate display name if provided
    if (payload.displayName && payload.displayName.length > 20) {
      setProfileFeedback({ type: "error", title: "Display name must be at most 20 characters" })
      return;
    }

    try {
      await withPageLoading("profile", async () => {
        const success = await apiClient.updateCurrentUser(payload)
        if (success) {
          const current = await apiClient.getCurrentUser()
          syncProfile(current)
          setProfileFeedback({ type: "success", title: "Profile updated successfully" })
          
          // Update local storage
          localStorage.setItem("currentUser", JSON.stringify(current))
        } else {
          throw new Error("Failed to update profile")
        }
      })
    } catch (error) {
      console.error("Failed to update profile:", error)
      setProfileFeedback({ type: "error", title: error instanceof Error ? error.message : "Failed to update profile" })
    }
  }

  const handlePasswordSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setPasswordFeedback(null)

    if (!passwordForm.currentPassword || !passwordForm.newPassword) {
      setPasswordFeedback({
        type: "error",
        title: "请输入完整信息",
        description: "请填写当前密码和新密码。"
      })
      return
    }

    if (passwordForm.newPassword !== passwordForm.confirmPassword) {
      setPasswordFeedback({
        type: "error",
        title: "两次密码不一致",
        description: "请确认新密码输入一致。"
      })
      return
    }

    try {
      await withActionLoading("profile-password", async () => {
        try {
          const success = await apiClient.changePassword(passwordForm.currentPassword, passwordForm.newPassword)
          if (success) {
            setPasswordFeedback({
              type: "success",
              title: "密码已更新",
              description: "下次登录请使用新密码。"
            })
            setPasswordForm({ currentPassword: "", newPassword: "", confirmPassword: "" })
          }
        } catch (error) {
          console.error("Failed to change password:", error)
          const message = error instanceof Error ? error.message : "未知错误"
          setPasswordFeedback({
            type: "error",
            title: "修改失败",
            description: message
          })
          throw error
        }
      })
    } catch {
      // 已处理 loading
    }
  }

  const formatDate = (value?: string | null) => {
    if (!value) return "--"
    return dayjs(value).format("YYYY-MM-DD HH:mm")
  }

  return (
    <PageLoading page="profile">
      <PageHeader title="个人资料" />
      <div className="flex flex-1 flex-col gap-4 p-4 pt-0">
        {loadError && (
          <Alert variant="destructive">
            <AlertTitle>加载用户信息失败</AlertTitle>
            <AlertDescription className="flex items-center justify-between gap-4">
              <span>{loadError}</span>
              <Button variant="outline" size="sm" onClick={() => loadProfile().catch(() => {})}>
                重新加载
              </Button>
            </AlertDescription>
          </Alert>
        )}

        {profileFeedback && (
          <Alert variant={profileFeedback.type === "error" ? "destructive" : "default"}>
            <AlertTitle>{profileFeedback.title}</AlertTitle>
            {profileFeedback.description && (
              <AlertDescription>{profileFeedback.description}</AlertDescription>
            )}
          </Alert>
        )}

        {githubFeedback && (
          <Alert variant={githubFeedback.type === "error" ? "destructive" : "default"}>
            <AlertTitle>{githubFeedback.title}</AlertTitle>
            {githubFeedback.description && (
              <AlertDescription>{githubFeedback.description}</AlertDescription>
            )}
          </Alert>
        )}

        <div className="grid gap-4 lg:grid-cols-3">
          <Card className="lg:col-span-2">
            <CardHeader>
              <CardTitle>基础资料</CardTitle>
              <CardDescription>更新您的个人显示信息，方便团队成员识别。</CardDescription>
            </CardHeader>
            <CardContent>
              <form className="space-y-6" onSubmit={handleProfileSubmit}>
                <div className="flex items-center gap-4">
                  <Avatar className="h-16 w-16 rounded-xl">
                    <AvatarImage src={profile?.avatar || defaultAvatar} alt={profile?.displayName || profile?.username} />
                    <AvatarFallback className="rounded-xl text-lg font-semibold">{avatarFallback}</AvatarFallback>
                  </Avatar>
                  <div>
                    <div className="text-xl font-semibold">{profile?.displayName || profile?.username || ""}</div>
                    <div className="text-sm text-muted-foreground">{profile?.email || "--"}</div>
                  </div>
                </div>

                <Separator />

                <div className="grid gap-6 md:grid-cols-2">
                  <div className="space-y-2">
                    <Label htmlFor="displayName">显示名称</Label>
                    <Input
                      id="displayName"
                      value={profileForm.displayName}
                      onChange={(event) =>
                        setProfileForm((prev) => ({ ...prev, displayName: event.target.value }))
                      }
                      placeholder="请输入您希望展示的名称"
                    />
                  </div>
                </div>

                <div className="grid gap-6 md:grid-cols-2">
                  <div className="space-y-2">
                    <Label htmlFor="email">邮箱</Label>
                    <Input id="email" value={profile?.email || ""} disabled />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="username">用户名</Label>
                    <Input id="username" value={profile?.username || ""} disabled />
                  </div>
                </div>

                <div className="flex items-center justify-end gap-2">
                  <Button type="button" variant="outline" onClick={() => loadProfile().catch(() => {})} disabled={profileAction.loading}>
                    刷新
                  </Button>
                  <Button type="submit" disabled={profileAction.loading}>
                    {profileAction.loading && <LoadingSpinner size="sm" className="mr-2" />}
                    保存更改
                  </Button>
                </div>
              </form>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>账户信息</CardTitle>
              <CardDescription>查看当前账户状态、角色和安全信息。</CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <div>
                <div className="text-sm text-muted-foreground">角色</div>
                <div className="flex items-center gap-2 pt-1">
                  <Badge variant="secondary">{getRoleDisplayText(profile?.role || "USER")}</Badge>
                  <ShieldCheck className="h-4 w-4 text-muted-foreground" />
                </div>
              </div>
              <Separator />
              <div className="grid gap-2 text-sm">
                <div className="flex justify-between">
                  <span className="text-muted-foreground">创建时间</span>
                  <span>{formatDate(profile?.createdAt)}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-muted-foreground">最近登录</span>
                  <span>{formatDate(profile?.lastLoginAt)}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-muted-foreground">账号状态</span>
                  <span>{profile?.active ? "已启用" : "已停用"}</span>
                </div>
              </div>
              <Separator />
              <div className="space-y-3">
                <div className="text-sm text-muted-foreground">GitHub 绑定</div>
                <div className="flex flex-wrap items-center justify-between gap-4 rounded-lg border border-dashed border-muted-foreground/20 p-4">
                  <div className="flex items-center gap-3">
                    <div className="flex h-10 w-10 items-center justify-center rounded-full bg-muted">
                      <Github className="h-5 w-5 text-muted-foreground" />
                    </div>
                    <div>
                      <div className="flex items-center gap-2 text-sm font-medium">
                        {profile?.githubId ? "已绑定 GitHub" : "尚未绑定"}
                        <Badge variant={profile?.githubId ? "default" : "outline"}>
                          {profile?.githubId ? "已绑定" : "未绑定"}
                        </Badge>
                      </div>
                      <div className="text-xs text-muted-foreground">
                        {profile?.githubId ? `ID: ${githubIdDisplay}` : "绑定后可使用 GitHub 账号快速登录"}
                      </div>
                    </div>
                  </div>
                  <div className="flex items-center gap-2">
                    {profile?.githubId ? (
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={handleGitHubUnbind}
                        disabled={githubAction.loading}
                      >
                        {githubAction.loading && <LoadingSpinner size="sm" className="mr-2" />}
                        解绑
                      </Button>
                    ) : (
                      <Button size="sm" onClick={handleGitHubBind} disabled={githubAction.loading}>
                        绑定 GitHub
                      </Button>
                    )}
                  </div>
                </div>
                {profile?.githubId && (
                  <p className="text-xs text-muted-foreground">
                    如需更换 GitHub 账号，请先解绑后重新绑定。
                  </p>
                )}
              </div>
            </CardContent>
          </Card>
        </div>

        {passwordFeedback && (
          <Alert variant={passwordFeedback.type === "error" ? "destructive" : "default"}>
            <AlertTitle>{passwordFeedback.title}</AlertTitle>
            {passwordFeedback.description && (
              <AlertDescription>{passwordFeedback.description}</AlertDescription>
            )}
          </Alert>
        )}

        <Card>
          <CardHeader>
            <CardTitle>修改密码</CardTitle>
            <CardDescription>定期更新密码可以提升账号安全性。</CardDescription>
          </CardHeader>
          <CardContent>
            <form className="space-y-4" onSubmit={handlePasswordSubmit}>
              <div className="space-y-2">
                <Label htmlFor="currentPassword">当前密码</Label>
                <Input
                  id="currentPassword"
                  type="password"
                  value={passwordForm.currentPassword}
                  onChange={(event) =>
                    setPasswordForm((prev) => ({ ...prev, currentPassword: event.target.value }))
                  }
                  placeholder="输入当前密码"
                />
              </div>
              <div className="grid gap-4 md:grid-cols-2">
                <div className="space-y-2">
                  <Label htmlFor="newPassword">新密码</Label>
                  <Input
                    id="newPassword"
                    type="password"
                    value={passwordForm.newPassword}
                    onChange={(event) =>
                      setPasswordForm((prev) => ({ ...prev, newPassword: event.target.value }))
                    }
                    placeholder="至少 6 位"
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="confirmPassword">确认新密码</Label>
                  <Input
                    id="confirmPassword"
                    type="password"
                    value={passwordForm.confirmPassword}
                    onChange={(event) =>
                      setPasswordForm((prev) => ({ ...prev, confirmPassword: event.target.value }))
                    }
                    placeholder="再次输入新密码"
                  />
                </div>
              </div>
              <CardFooter className="px-0">
                <div className="ml-auto flex items-center gap-2">
                  <Button
                    type="submit"
                    disabled={passwordAction.loading}
                  >
                    {passwordAction.loading && <LoadingSpinner size="sm" className="mr-2" />}
                    更新密码
                  </Button>
                </div>
              </CardFooter>
            </form>
          </CardContent>
        </Card>
      </div>
    </PageLoading>
  )
}
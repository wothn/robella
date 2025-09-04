import React, { useState, useEffect } from 'react'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { UsersIcon, PlusIcon, SearchIcon } from 'lucide-react'
import { apiClient } from '@/lib/api'
import type { User } from '@/lib/api'

const UserManagement: React.FC = () => {
  const [users, setUsers] = useState<User[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [searchTerm, setSearchTerm] = useState('')

  // 加载用户列表
  const loadUsers = async () => {
    try {
      setLoading(true)
      setError('')
      const userList = await apiClient.getAllUsers()
      setUsers(userList)
    } catch (err) {
      setError('加载用户列表失败')
      console.error('加载用户失败:', err)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadUsers()
  }, [])

  // 过滤用户
  const filteredUsers = users.filter(user =>
    user.username.toLowerCase().includes(searchTerm.toLowerCase()) ||
    user.email.toLowerCase().includes(searchTerm.toLowerCase()) ||
    user.role.toLowerCase().includes(searchTerm.toLowerCase())
  )

  const handleActivateUser = async (id: number) => {
    try {
      await apiClient.activateUser(id)
      console.log('用户激活成功:', id)
      await loadUsers() // 重新加载用户列表
    } catch (err) {
      console.error('激活用户失败:', err)
      setError('激活用户失败')
    }
  }

  const handleDeactivateUser = async (id: number) => {
    try {
      await apiClient.deactivateUser(id)
      console.log('用户停用成功:', id)
      await loadUsers() // 重新加载用户列表
    } catch (err) {
      console.error('停用用户失败:', err)
      setError('停用用户失败')
    }
  }

  return (
    <div className="space-y-6">
      <Card>
        <CardHeader>
          <div className="flex items-center space-x-2">
            <UsersIcon className="h-6 w-6" />
            <CardTitle>用户管理</CardTitle>
          </div>
          <CardDescription>
            管理系统用户账户
          </CardDescription>
        </CardHeader>
        <CardContent>
          <div className="flex items-center space-x-4 mb-6">
            <div className="flex-1">
              <Label htmlFor="search">搜索用户</Label>
              <div className="relative">
                <SearchIcon className="absolute left-3 top-3 h-4 w-4 text-muted-foreground" />
                <Input
                  id="search"
                  placeholder="按用户名、邮箱或角色搜索..."
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  className="pl-10"
                />
              </div>
            </div>
            <Button onClick={loadUsers} variant="outline">
              刷新
            </Button>
            <Button>
              <PlusIcon className="h-4 w-4 mr-2" />
              添加用户
            </Button>
          </div>

          {error && (
            <div className="text-sm text-destructive bg-destructive/10 border border-destructive/20 rounded-md p-3 mb-4">
              {error}
            </div>
          )}

          {loading ? (
            <div className="text-center py-8">
              <p className="text-muted-foreground">加载中...</p>
            </div>
          ) : (
            <div className="rounded-md border">
              <table className="w-full">
                <thead>
                  <tr className="border-b bg-muted/50">
                    <th className="p-4 text-left">用户名</th>
                    <th className="p-4 text-left">邮箱</th>
                    <th className="p-4 text-left">角色</th>
                    <th className="p-4 text-left">状态</th>
                    <th className="p-4 text-left">最后登录</th>
                    <th className="p-4 text-left">操作</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredUsers.length === 0 ? (
                    <tr>
                      <td colSpan={6} className="p-8 text-center text-muted-foreground">
                        {searchTerm ? '没有找到匹配的用户' : '暂无用户数据'}
                      </td>
                    </tr>
                  ) : (
                    filteredUsers.map((user) => (
                      <tr key={user.id} className="border-b">
                        <td className="p-4 font-medium">{user.username}</td>
                        <td className="p-4">{user.email}</td>
                        <td className="p-4">
                          <span className="inline-flex items-center rounded-full px-2 py-1 text-xs font-medium bg-blue-100 text-blue-800">
                            {user.role}
                          </span>
                        </td>
                        <td className="p-4">
                          <span className={`inline-flex items-center rounded-full px-2 py-1 text-xs font-medium ${
                            user.active 
                              ? 'bg-green-100 text-green-800' 
                              : 'bg-red-100 text-red-800'
                          }`}>
                            {user.active ? '活跃' : '停用'}
                          </span>
                        </td>
                        <td className="p-4 text-sm text-muted-foreground">
                          {user.lastLoginAt ? new Date(user.lastLoginAt).toLocaleString() : '从未登录'}
                        </td>
                        <td className="p-4">
                          <div className="flex space-x-2">
                            {user.active ? (
                              <Button
                                size="sm"
                                variant="outline"
                                onClick={() => handleDeactivateUser(user.id)}
                              >
                                停用
                              </Button>
                            ) : (
                              <Button
                                size="sm"
                                variant="outline"
                                onClick={() => handleActivateUser(user.id)}
                              >
                                激活
                              </Button>
                            )}
                            <Button size="sm" variant="outline">
                              编辑
                            </Button>
                          </div>
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  )
}

export default UserManagement

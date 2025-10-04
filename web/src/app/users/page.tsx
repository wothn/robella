import { PageHeader } from "@/components/layout/page-header"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Badge } from "@/components/ui/badge"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { Label } from "@/components/ui/label"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { useState, useEffect } from "react"
import { apiClient } from "@/lib/api"
import { usePageLoading, withPageLoading, withActionLoading, useActionLoading } from "@/stores/loading-store"
import { PageLoading, LoadingButton } from "@/components/common/loading"
import type { User, CreateUserRequest } from "@/types/user"
import { getRoleDisplayText } from "@/types/user"
import dayjs from "dayjs"

export default function UsersPage() {
  const [users, setUsers] = useState<User[]>([])
  const [searchTerm, setSearchTerm] = useState("")
  const [selectedUser, setSelectedUser] = useState<User | null>(null)
  const [isDialogOpen, setIsDialogOpen] = useState(false)
  const [isCreateDialogOpen, setIsCreateDialogOpen] = useState(false)
  const [isEditDialogOpen, setIsEditDialogOpen] = useState(false)
  const [isDeleteDialogOpen, setIsDeleteDialogOpen] = useState(false)
  const { loading } = usePageLoading('users')
  const createLoading = useActionLoading('create-user')
  const updateLoading = useActionLoading('update-user')
  const deleteLoading = useActionLoading('delete-user')
  
  // Form states
  const [formData, setFormData] = useState({
    username: "",
    email: "",
    password: "",
    displayName: "",
    role: 'USER',
    active: true,
    credits: 0
  })

  useEffect(() => {
    fetchUsers()
  }, [])

  const fetchUsers = async () => {
    return withPageLoading('users', async () => {
      try {
        const response = await apiClient.getAllUsers()
        setUsers(response)
      } catch (error) {
        console.error("Failed to fetch users:", error)
      }
    })
  }

  const handleToggleUserStatus = async (userId: number, isActive: boolean) => {
    try {
      if (isActive) {
        const success = await apiClient.deactivateUser(userId)
        if (success) {
          await fetchUsers()
        }
      } else {
        const success = await apiClient.activateUser(userId)
        if (success) {
          await fetchUsers()
        }
      }
    } catch (error) {
      console.error("Failed to toggle user status:", error)
    }
  }

  const handleCreateUser = async () => {
    return withActionLoading('create-user', async () => {
      try {
        // Validate input data
        const username = formData.username.trim();
        const password = formData.password;
        const displayName = formData.displayName.trim();
        
        // Username validation
        if (!username) {
          alert("Username is required");
          return;
        }
        if (username.length > 20) {
          alert("Username must be at most 20 characters");
          return;
        }
        
        // Password validation
        if (!password) {
          alert("Password is required");
          return;
        }
        if (password.length < 6) {
          alert("Password must be at least 6 characters");
          return;
        }
        
        // Display name validation
        if (displayName && displayName.length > 20) {
          alert("Display name must be at most 20 characters");
          return;
        }

        const createData: CreateUserRequest = {
          username: username,
          email: formData.email,
          password: password,
          displayName: displayName || undefined,
          role: formData.role
        }

        await apiClient.createUser(createData)
        await fetchUsers()
        setIsCreateDialogOpen(false)
        resetForm()
      } catch (error) {
        console.error("Failed to create user:", error)
      }
    })
  }

  const handleUpdateUser = async () => {
    if (!selectedUser) return

    return withActionLoading('update-user', async () => {
      try {
        const displayName = formData.displayName?.trim();
        
        // Validate display name if provided
        if (displayName && displayName.length > 20) {
          alert("Display name must be at most 20 characters");
          return;
        }

        const updateData: Partial<User> = {
          displayName: displayName,
          role: formData.role,
          active: formData.active,
          credits: formData.credits
        }

        const success = await apiClient.updateUser(selectedUser.id, updateData)
        if (success) {
          await fetchUsers()
          setIsEditDialogOpen(false)
          resetForm()
        }
      } catch (error) {
        console.error("Failed to update user:", error)
      }
    })
  }

  const handleDeleteUser = async () => {
    if (!selectedUser) return

    return withActionLoading('delete-user', async () => {
      try {
        const success = await apiClient.deleteUser(selectedUser.id)
        if (success) {
          await fetchUsers()
          setIsDeleteDialogOpen(false)
          setSelectedUser(null)
        }
      } catch (error) {
        console.error("Failed to delete user:", error)
      }
    })
  }

  const openViewDialog = async (userId: number) => {
    try {
      const userDetail = await apiClient.getUserById(userId)
      setSelectedUser(userDetail)
      setIsDialogOpen(true)
    } catch (error) {
      console.error("Failed to fetch user details:", error)
    }
  }

  const openEditDialog = async (userId: number) => {
    try {
      const userDetail = await apiClient.getUserById(userId)
      setSelectedUser(userDetail)
      setFormData({
        username: userDetail.username,
        email: userDetail.email,
        password: "",
        displayName: userDetail.displayName,
        role: userDetail.role,
        active: userDetail.active,
        credits: userDetail.credits || 0
      })
      setIsEditDialogOpen(true)
    } catch (error) {
      console.error("Failed to fetch user details:", error)
    }
  }

  const openDeleteDialog = (user: User) => {
    setSelectedUser(user)
    setIsDeleteDialogOpen(true)
  }

  const resetForm = () => {
    setFormData({
      username: "",
      email: "",
      password: "",
      displayName: "",
      role: 'USER',
      active: true,
      credits: 0
    })
    setSelectedUser(null)
  }

  const filteredUsers = users.filter(user =>
    user.username.toLowerCase().includes(searchTerm.toLowerCase()) ||
    user.email.toLowerCase().includes(searchTerm.toLowerCase()) ||
    user.displayName.toLowerCase().includes(searchTerm.toLowerCase())
  )

  return (
    <PageLoading page="users">
      <PageHeader title="用户管理" />

      <div className="flex flex-1 flex-col gap-4 p-4 pt-0">
          <div className="flex items-center justify-between">
            <h1 className="text-2xl font-bold">User Management</h1>
            <div className="flex items-center gap-2">
              <Button
                onClick={() => {
                  resetForm()
                  setIsCreateDialogOpen(true)
                }}
              >
                Add User
              </Button>
              <Input
                placeholder="Search users..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="w-64"
              />
            </div>
          </div>

          <div className="border rounded-lg">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>ID</TableHead>
                  <TableHead>Username</TableHead>
                  <TableHead>Email</TableHead>
                  <TableHead>Display Name</TableHead>
                  <TableHead>Role</TableHead>
                  <TableHead>Status</TableHead>
                  <TableHead>Last Login</TableHead>
                  <TableHead>Actions</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {loading ? (
                  <TableRow>
                    <TableCell colSpan={8} className="text-center py-8">
                      Loading users...
                    </TableCell>
                  </TableRow>
                ) : filteredUsers.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={8} className="text-center py-8">
                      No users found
                    </TableCell>
                  </TableRow>
                ) : (
                  filteredUsers.map((user) => (
                    <TableRow key={user.id}>
                      <TableCell>{user.id}</TableCell>
                      <TableCell className="font-medium">{user.username}</TableCell>
                      <TableCell>{user.email}</TableCell>
                      <TableCell>{user.displayName}</TableCell>
                      <TableCell>
                        <Badge variant="outline">{getRoleDisplayText(user.role)}</Badge>
                      </TableCell>
                      <TableCell>
                        <Badge variant={user.active ? "default" : "secondary"}>
                          {user.active ? "Active" : "Inactive"}
                        </Badge>
                      </TableCell>
                      <TableCell>
                        {user.lastLoginAt
                          ? dayjs(user.lastLoginAt).format("MMM DD, YYYY")
                          : "Never"}
                      </TableCell>
                      <TableCell>
                        <div className="flex items-center gap-2">
                          <Button
                            variant="outline"
                            size="sm"
                            onClick={() => openViewDialog(user.id)}
                          >
                            View
                          </Button>
                          <Button
                            variant="outline"
                            size="sm"
                            onClick={() => openEditDialog(user.id)}
                          >
                            Edit
                          </Button>
                          <Button
                            variant={user.active ? "destructive" : "default"}
                            size="sm"
                            onClick={() => handleToggleUserStatus(user.id, user.active)}
                          >
                            {user.active ? "Deactivate" : "Activate"}
                          </Button>
                          <Button
                            variant="destructive"
                            size="sm"
                            onClick={() => openDeleteDialog(user)}
                          >
                            Delete
                          </Button>
                        </div>
                      </TableCell>
                    </TableRow>
                  ))
                )}
              </TableBody>
            </Table>
          </div>

          <Dialog open={isDialogOpen} onOpenChange={setIsDialogOpen}>
            <DialogContent className="max-w-md">
              <DialogHeader>
                <DialogTitle>User Details</DialogTitle>
                <DialogDescription>
                  Detailed information about the user
                </DialogDescription>
              </DialogHeader>
              {selectedUser && (
                <div className="space-y-4">
                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <Label className="text-sm font-medium">ID</Label>
                      <p className="text-sm text-muted-foreground">{selectedUser.id}</p>
                    </div>
                    <div>
                      <Label className="text-sm font-medium">Username</Label>
                      <p className="text-sm text-muted-foreground">{selectedUser.username}</p>
                    </div>
                    <div>
                      <Label className="text-sm font-medium">Email</Label>
                      <p className="text-sm text-muted-foreground">{selectedUser.email}</p>
                    </div>
                    <div>
                      <Label className="text-sm font-medium">Full Name</Label>
                      <p className="text-sm text-muted-foreground">{selectedUser.displayName}</p>
                    </div>
                    <div>
                      <Label className="text-sm font-medium">Role</Label>
                      <p className="text-sm text-muted-foreground">{getRoleDisplayText(selectedUser.role)}</p>
                    </div>
                    <div>
                      <Label className="text-sm font-medium">Status</Label>
                      <p className="text-sm text-muted-foreground">
                        {selectedUser.active ? "Active" : "Inactive"}
                      </p>
                    </div>
                    <div>
                      <Label className="text-sm font-medium">Credits</Label>
                      <p className="text-sm text-muted-foreground">{selectedUser.credits || 0}</p>
                    </div>
                  </div>
                  <div>
                    <Label className="text-sm font-medium">Created At</Label>
                    <p className="text-sm text-muted-foreground">
                      {dayjs(selectedUser.createdAt).format("MMMM DD, YYYY [at] HH:mm")}
                    </p>
                  </div>
                  <div>
                    <Label className="text-sm font-medium">Last Login</Label>
                    <p className="text-sm text-muted-foreground">
                      {selectedUser.lastLoginAt
                        ? dayjs(selectedUser.lastLoginAt).format("MMMM DD, YYYY [at] HH:mm")
                        : "Never"}
                    </p>
                  </div>
                  {selectedUser.githubId && (
                    <div>
                      <Label className="text-sm font-medium">GitHub ID</Label>
                      <p className="text-sm text-muted-foreground">{selectedUser.githubId}</p>
                    </div>
                  )}
                </div>
              )}
              <DialogFooter>
                <Button variant="outline" onClick={() => setIsDialogOpen(false)}>
                  Close
                </Button>
              </DialogFooter>
            </DialogContent>
          </Dialog>

          {/* Create User Dialog */}
          <Dialog open={isCreateDialogOpen} onOpenChange={setIsCreateDialogOpen}>
            <DialogContent className="max-w-md">
              <DialogHeader>
                <DialogTitle>Create New User</DialogTitle>
                <DialogDescription>
                  Fill in the details to create a new user account
                </DialogDescription>
              </DialogHeader>
              <div className="space-y-4">
                <div>
                  <Label htmlFor="username">Username</Label>
                  <Input
                    id="username"
                    value={formData.username}
                    onChange={(e) => setFormData({...formData, username: e.target.value})}
                    placeholder="Enter username"
                  />
                </div>
                <div>
                  <Label htmlFor="email">Email</Label>
                  <Input
                    id="email"
                    type="email"
                    value={formData.email}
                    onChange={(e) => setFormData({...formData, email: e.target.value})}
                    placeholder="Enter email"
                  />
                </div>
                <div>
                  <Label htmlFor="password">Password</Label>
                  <Input
                    id="password"
                    type="password"
                    value={formData.password}
                    onChange={(e) => setFormData({...formData, password: e.target.value})}
                    placeholder="Enter password"
                  />
                </div>
                <div>
                  <Label htmlFor="displayName">Display Name</Label>
                  <Input
                    id="displayName"
                    value={formData.displayName}
                    onChange={(e) => setFormData({...formData, displayName: e.target.value})}
                    placeholder="Enter display name"
                  />
                </div>
                <div>
                  <Label htmlFor="role">Role</Label>
                  <Select value={formData.role || "USER"} onValueChange={(value) => setFormData({...formData, role: value})}>
                    <SelectTrigger>
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="GUEST">Guest</SelectItem>
                      <SelectItem value="USER">User</SelectItem>
                      <SelectItem value="ADMIN">Admin</SelectItem>
                      <SelectItem value="ROOT">Root</SelectItem>
                    </SelectContent>
                  </Select>
                </div>
              </div>
              <DialogFooter>
                <Button variant="outline" onClick={() => setIsCreateDialogOpen(false)}>
                  Cancel
                </Button>
                <LoadingButton
                  loading={createLoading.loading}
                  onClick={handleCreateUser}
                >
                  Create User
                </LoadingButton>
              </DialogFooter>
            </DialogContent>
          </Dialog>

          {/* Edit User Dialog */}
          <Dialog open={isEditDialogOpen} onOpenChange={setIsEditDialogOpen}>
            <DialogContent className="max-w-md">
              <DialogHeader>
                <DialogTitle>Edit User</DialogTitle>
                <DialogDescription>
                  Update user information and settings
                </DialogDescription>
              </DialogHeader>
              {selectedUser && (
                <div className="space-y-4">
                  <div>
                    <Label htmlFor="edit-username">Username</Label>
                    <Input
                      id="edit-username"
                      value={formData.username}
                      disabled
                      className="bg-muted"
                    />
                  </div>
                  <div>
                    <Label htmlFor="edit-email">Email</Label>
                    <Input
                      id="edit-email"
                      value={formData.email}
                      disabled
                      className="bg-muted"
                    />
                  </div>
                  <div>
                    <Label htmlFor="edit-displayName">Display Name</Label>
                    <Input
                      id="edit-displayName"
                      value={formData.displayName}
                      onChange={(e) => setFormData({...formData, displayName: e.target.value})}
                      placeholder="Enter display name"
                    />
                  </div>
                  <div>
                    <Label htmlFor="edit-role">Role</Label>
                    <Select value={formData.role || "USER"} onValueChange={(value) => setFormData({...formData, role: value})}>
                      <SelectTrigger>
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="GUEST">Guest</SelectItem>
                        <SelectItem value="USER">User</SelectItem>
                        <SelectItem value="ADMIN">Admin</SelectItem>
                        <SelectItem value="ROOT">Root</SelectItem>
                      </SelectContent>
                    </Select>
                  </div>
                  <div>
                    <Label htmlFor="edit-active">Status</Label>
                    <Select value={formData.active ? "true" : "false"} onValueChange={(value) => setFormData({...formData, active: value === "true"})}>
                      <SelectTrigger>
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="true">Active</SelectItem>
                        <SelectItem value="false">Inactive</SelectItem>
                      </SelectContent>
                    </Select>
                  </div>
                  <div>
                    <Label htmlFor="edit-credits">Credits</Label>
                    <Input
                      id="edit-credits"
                      type="number"
                      value={formData.credits || 0}
                      onChange={(e) => setFormData({...formData, credits: Number(e.target.value)})}
                      placeholder="Enter credits"
                    />
                  </div>
                  
                </div>
              )}
              <DialogFooter>
                <Button variant="outline" onClick={() => setIsEditDialogOpen(false)}>
                  Cancel
                </Button>
                <LoadingButton
                  loading={updateLoading.loading}
                  onClick={handleUpdateUser}
                >
                  Update User
                </LoadingButton>
              </DialogFooter>
            </DialogContent>
          </Dialog>

          {/* Delete User Dialog */}
          <Dialog open={isDeleteDialogOpen} onOpenChange={setIsDeleteDialogOpen}>
            <DialogContent className="max-w-md">
              <DialogHeader>
                <DialogTitle>Delete User</DialogTitle>
                <DialogDescription>
                  Are you sure you want to delete this user? This action cannot be undone.
                </DialogDescription>
              </DialogHeader>
              {selectedUser && (
                <div className="space-y-2">
                  <p><strong>Username:</strong> {selectedUser.username}</p>
                  <p><strong>Email:</strong> {selectedUser.email}</p>
                  <p><strong>Display Name:</strong> {selectedUser.displayName}</p>
                </div>
              )}
              <DialogFooter>
                <Button variant="outline" onClick={() => setIsDeleteDialogOpen(false)}>
                  Cancel
                </Button>
                <LoadingButton
                  loading={deleteLoading.loading}
                  variant="destructive"
                  onClick={handleDeleteUser}
                >
                  Delete User
                </LoadingButton>
              </DialogFooter>
            </DialogContent>
          </Dialog>
        </div>
    </PageLoading>
  )
}
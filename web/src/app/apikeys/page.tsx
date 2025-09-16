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
import { Textarea } from "@/components/ui/textarea"
import { Switch } from "@/components/ui/switch"
import { useApiKeys } from "@/hooks/use-apikeys"
import type { ApiKey, ApiKeyCreateRequest } from "@/types/apikey"
import { format } from "date-fns"
import { useState } from "react"
import { Copy, Plus, Trash2} from "lucide-react"

export default function ApiKeysPage() {
  const {
    apiKeys,
    loading,
    error,
    createApiKey,
    deleteApiKey,
    toggleApiKeyStatus
  } = useApiKeys()

  const [searchTerm, setSearchTerm] = useState("")
  const [isCreateDialogOpen, setIsCreateDialogOpen] = useState(false)
  const [isDeleteDialogOpen, setIsDeleteDialogOpen] = useState(false)
  const [isShowKeyDialogOpen, setIsShowKeyDialogOpen] = useState(false)
  const [selectedKey, setSelectedKey] = useState<ApiKey | null>(null)
  const [newApiKey, setNewApiKey] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [copiedNewKey, setCopiedNewKey] = useState(false)

  // Form states
  const [formData, setFormData] = useState({
    name: "",
    description: "",
    dailyLimit: null as number | null,
    monthlyLimit: null as number | null,
    rateLimit: 60
  })

  // Filter API keys based on search term
  const filteredApiKeys = apiKeys.filter(key =>
    key.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
    key.description.toLowerCase().includes(searchTerm.toLowerCase())
  )

  const handleCreateKey = async () => {
    if (!formData.name.trim()) return

    setIsSubmitting(true)
    try {
      const createData: ApiKeyCreateRequest = {
        name: formData.name.trim(),
        description: formData.description.trim(),
        dailyLimit: formData.dailyLimit,
        monthlyLimit: formData.monthlyLimit,
        rateLimit: formData.rateLimit
      }

      const result = await createApiKey(createData)
      setNewApiKey(result.apiKey)
      setIsCreateDialogOpen(false)
      setIsShowKeyDialogOpen(true)
      resetForm()
    } catch (error) {
      console.error("Error creating API key:", error)
    } finally {
      setIsSubmitting(false)
    }
  }

  const handleDeleteKey = async () => {
    if (!selectedKey) return

    setIsSubmitting(true)
    try {
      await deleteApiKey(selectedKey.id)
      setIsDeleteDialogOpen(false)
      setSelectedKey(null)
    } catch (error) {
      console.error("Error deleting API key:", error)
    } finally {
      setIsSubmitting(false)
    }
  }

  const handleToggleStatus = async (id: number) => {
    try {
      await toggleApiKeyStatus(id)
    } catch (error) {
      console.error("Error toggling API key status:", error)
    }
  }

  const resetForm = () => {
    setFormData({
      name: "",
      description: "",
      dailyLimit: null,
      monthlyLimit: null,
      rateLimit: 60
    })
  }

  
  const copyNewKeyToClipboard = async () => {
    if (!newApiKey) return
    try {
      await navigator.clipboard.writeText(newApiKey)
      setCopiedNewKey(true)
      setTimeout(() => setCopiedNewKey(false), 2000)
    } catch (error) {
      console.error("Error copying to clipboard:", error)
    }
  }

  return (
    <div className="space-y-6">
      <PageHeader title="API Keys" />

      <div className="flex items-center justify-between">
        <p className="text-muted-foreground">Manage your API keys for accessing the Robella API</p>
        <Button onClick={() => setIsCreateDialogOpen(true)}>
          <Plus className="mr-2 h-4 w-4" />
          Create API Key
        </Button>
      </div>

      {/* Search and Filter */}
      <div className="flex items-center space-x-4">
        <Input
          placeholder="Search API keys..."
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
          className="max-w-sm"
        />
      </div>

      {/* Error Display */}
      {error && (
        <div className="bg-red-50 border border-red-200 rounded-lg p-4 text-red-800">
          <p className="font-medium">Error</p>
          <p className="text-sm">{error}</p>
        </div>
      )}

      {/* API Keys Table */}
      <div className="rounded-lg border bg-card">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Name</TableHead>
              <TableHead>Description</TableHead>
              <TableHead>Key</TableHead>
              <TableHead>Status</TableHead>
              <TableHead>Rate Limit</TableHead>
              <TableHead>Daily Limit</TableHead>
              <TableHead>Monthly Limit</TableHead>
              <TableHead>Created</TableHead>
              <TableHead>Actions</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {loading ? (
              <TableRow>
                <TableCell colSpan={9} className="text-center py-8">
                  <div className="flex items-center justify-center space-x-2">
                    <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-primary"></div>
                    <span>Loading API keys...</span>
                  </div>
                </TableCell>
              </TableRow>
            ) : filteredApiKeys.length === 0 ? (
              <TableRow>
                <TableCell colSpan={9} className="text-center py-8">
                  <p className="text-muted-foreground">No API keys found</p>
                </TableCell>
              </TableRow>
            ) : (
              filteredApiKeys.map((key) => (
                <TableRow key={key.id}>
                  <TableCell className="font-medium">{key.name}</TableCell>
                  <TableCell>{key.description}</TableCell>
                  <TableCell>
                    <code className="bg-muted px-2 py-1 rounded text-sm font-mono">
                      {key.keyPrefix}...
                    </code>
                  </TableCell>
                  <TableCell>
                    <Badge variant={key.active ? "default" : "secondary"}>
                      {key.active ? "Active" : "Inactive"}
                    </Badge>
                  </TableCell>
                  <TableCell>{key.rateLimit}/min</TableCell>
                  <TableCell>{key.dailyLimit || "Unlimited"}</TableCell>
                  <TableCell>{key.monthlyLimit || "Unlimited"}</TableCell>
                  <TableCell>
                    {format(new Date(key.createdAt), "MMM d, yyyy")}
                  </TableCell>
                  <TableCell>
                    <div className="flex items-center space-x-2">
                      <Switch
                        checked={key.active}
                        onCheckedChange={() => handleToggleStatus(key.id)}
                      />
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => {
                          setSelectedKey(key)
                          setIsDeleteDialogOpen(true)
                        }}
                      >
                        <Trash2 className="h-4 w-4" />
                      </Button>
                    </div>
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </div>

      {/* Create API Key Dialog */}
      <Dialog open={isCreateDialogOpen} onOpenChange={setIsCreateDialogOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Create New API Key</DialogTitle>
            <DialogDescription>
              Generate a new API key for accessing the Robella API.
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-4">
            <div>
              <Label htmlFor="name">Name *</Label>
              <Input
                id="name"
                placeholder="Enter API key name"
                value={formData.name}
                onChange={(e) => setFormData({ ...formData, name: e.target.value })}
              />
            </div>
            <div>
              <Label htmlFor="description">Description</Label>
              <Textarea
                id="description"
                placeholder="Enter API key description"
                value={formData.description}
                onChange={(e) => setFormData({ ...formData, description: e.target.value })}
              />
            </div>
            <div className="grid grid-cols-3 gap-4">
              <div>
                <Label htmlFor="rateLimit">Rate Limit (req/min)</Label>
                <Input
                  id="rateLimit"
                  type="number"
                  placeholder="60"
                  value={formData.rateLimit}
                  onChange={(e) => setFormData({ ...formData, rateLimit: parseInt(e.target.value) || 60 })}
                />
              </div>
              <div>
                <Label htmlFor="dailyLimit">Daily Limit</Label>
                <Input
                  id="dailyLimit"
                  type="number"
                  placeholder="Unlimited"
                  value={formData.dailyLimit || ""}
                  onChange={(e) => setFormData({ ...formData, dailyLimit: e.target.value ? parseInt(e.target.value) : null })}
                />
              </div>
              <div>
                <Label htmlFor="monthlyLimit">Monthly Limit</Label>
                <Input
                  id="monthlyLimit"
                  type="number"
                  placeholder="Unlimited"
                  value={formData.monthlyLimit || ""}
                  onChange={(e) => setFormData({ ...formData, monthlyLimit: e.target.value ? parseInt(e.target.value) : null })}
                />
              </div>
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setIsCreateDialogOpen(false)}>
              Cancel
            </Button>
            <Button
              onClick={handleCreateKey}
              disabled={!formData.name.trim() || isSubmitting}
            >
              {isSubmitting ? "Creating..." : "Create API Key"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Delete API Key Dialog */}
      <Dialog open={isDeleteDialogOpen} onOpenChange={setIsDeleteDialogOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Delete API Key</DialogTitle>
            <DialogDescription>
              你确定要删除 API 密钥 {selectedKey?.name} 吗？此操作无法撤消。
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={() => setIsDeleteDialogOpen(false)}>
              取消
            </Button>
            <Button
              variant="destructive"
              onClick={handleDeleteKey}
              disabled={isSubmitting}
            >
              {isSubmitting ? "Deleting..." : "Delete API Key"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Show New API Key Dialog */}
      <Dialog open={isShowKeyDialogOpen} onOpenChange={setIsShowKeyDialogOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>API Key Created Successfully</DialogTitle>
            <DialogDescription>
              Your new API key has been generated. Please copy it now as it will only be shown once.
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-4">
            <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4">
              <div className="flex">
                <div className="flex-shrink-0">
                  <svg className="h-5 w-5 text-yellow-400" viewBox="0 0 20 20" fill="currentColor">
                    <path fillRule="evenodd" d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z" clipRule="evenodd" />
                  </svg>
                </div>
                <div className="ml-3">
                  <h3 className="text-sm font-medium text-yellow-800">
                    Important: Save Your API Key
                  </h3>
                  <div className="mt-2 text-sm text-yellow-700">
                    <p>
                      Your API key is displayed below. Please copy and store it in a secure location.
                      For security reasons, we won't be able to show you this key again.
                    </p>
                  </div>
                </div>
              </div>
            </div>

            <div>
              <Label htmlFor="apiKey">API Key</Label>
              <div className="flex items-center space-x-2 mt-1">
                <code className="bg-muted px-3 py-2 rounded text-sm font-mono flex-1 break-all">
                  {newApiKey}
                </code>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={copyNewKeyToClipboard}
                  className="flex-shrink-0"
                >
                  <Copy className="h-4 w-4 mr-2" />
                  {copiedNewKey ? "Copied!" : "Copy"}
                </Button>
              </div>
            </div>
          </div>
          <DialogFooter>
            <Button
              onClick={() => {
                setIsShowKeyDialogOpen(false)
                setNewApiKey(null)
                setCopiedNewKey(false)
              }}
            >
              I've Saved My API Key
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}
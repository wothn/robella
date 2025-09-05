import React, { useState, useEffect } from 'react'
import { 
  Card, 
  Button, 
  Tag, 
  Table, 
  Alert, 
  Modal, 
  Avatar, 
  Select, 
  Form,
  Input,
  Space,
  Typography,
  Popconfirm,
  notification,
  Spin
} from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { 
  UserOutlined,
  UserAddOutlined, 
  EditOutlined, 
  DeleteOutlined, 
  CheckOutlined, 
  CloseOutlined,
  MailOutlined,
  CalendarOutlined,
  SecurityScanOutlined
} from '@ant-design/icons'

const { Title } = Typography
const { Option } = Select

interface User {
  id: number
  username: string
  email: string
  role: string
  active: boolean
  emailVerified: boolean
  phoneVerified: boolean
  createdAt: string
  updatedAt: string
  lastLoginAt?: string
  avatar?: string
  fullName?: string
  phone?: string
  githubId?: string
  provider?: string
  providerId?: string
}

interface UserFormData {
  username: string
  email: string
  password: string
  role: string
}

const UserManagement: React.FC = () => {
  const [users, setUsers] = useState<User[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [showAddModal, setShowAddModal] = useState(false)
  const [showEditModal, setShowEditModal] = useState(false)
  const [editingUser, setEditingUser] = useState<User | null>(null)
  const [form] = Form.useForm()

  const API_BASE = '/api/users'

  useEffect(() => {
    fetchUsers()
  }, [])

  const fetchUsers = async () => {
    try {
      setLoading(true)
      const token = localStorage.getItem('accessToken')
      const response = await fetch(API_BASE, {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      })
      
      if (!response.ok) {
        throw new Error('获取用户列表失败')
      }
      
      const data = await response.json()
      setUsers(data)
      setError(null)
    } catch (err) {
      setError(err instanceof Error ? err.message : '获取用户列表失败')
      notification.error({
        message: '错误',
        description: err instanceof Error ? err.message : '获取用户列表失败'
      })
    } finally {
      setLoading(false)
    }
  }

  const handleSubmit = async (values: UserFormData) => {
    try {
      const token = localStorage.getItem('accessToken')
      const url = editingUser ? `${API_BASE}/${editingUser.id}` : API_BASE
      const method = editingUser ? 'PUT' : 'POST'
      
      const response = await fetch(url, {
        method,
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify(values)
      })
      
      if (!response.ok) {
        throw new Error(editingUser ? '更新用户失败' : '创建用户失败')
      }
      
      await fetchUsers()
      
      if (editingUser) {
        setShowEditModal(false)
        setEditingUser(null)
        notification.success({
          message: '成功',
          description: '用户更新成功'
        })
      } else {
        setShowAddModal(false)
        notification.success({
          message: '成功',
          description: '用户创建成功'
        })
      }
      
      form.resetFields()
    } catch (err) {
      notification.error({
        message: '错误',
        description: err instanceof Error ? err.message : '操作失败'
      })
    }
  }

  const handleEdit = (user: User) => {
    setEditingUser(user)
    form.setFieldsValue({
      username: user.username,
      email: user.email,
      password: '',
      role: user.role
    })
    setShowEditModal(true)
  }

  const handleDelete = async (userId: number) => {
    try {
      const token = localStorage.getItem('accessToken')
      const response = await fetch(`${API_BASE}/${userId}`, {
        method: 'DELETE',
        headers: {
          'Authorization': `Bearer ${token}`
        }
      })
      
      if (!response.ok) {
        throw new Error('删除用户失败')
      }
      
      await fetchUsers()
      notification.success({
        message: '成功',
        description: '用户删除成功'
      })
    } catch (err) {
      notification.error({
        message: '错误',
        description: err instanceof Error ? err.message : '删除用户失败'
      })
    }
  }

  const handleToggleActive = async (userId: number, currentActive: boolean) => {
    try {
      const token = localStorage.getItem('accessToken')
      const endpoint = currentActive ? 'deactivate' : 'activate'
      const response = await fetch(`${API_BASE}/${userId}/${endpoint}`, {
        method: 'PUT',
        headers: {
          'Authorization': `Bearer ${token}`
        }
      })
      
      if (!response.ok) {
        throw new Error(`${currentActive ? '停用' : '激活'}用户失败`)
      }
      
      await fetchUsers()
      notification.success({
        message: '成功',
        description: `用户${currentActive ? '停用' : '激活'}成功`
      })
    } catch (err) {
      notification.error({
        message: '错误',
        description: err instanceof Error ? err.message : '操作失败'
      })
    }
  }

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleString('zh-CN')
  }

  const columns: ColumnsType<User> = [
    {
      title: '用户信息',
      key: 'userInfo',
      render: (_, user) => (
        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          <Avatar 
            size={40}
            src={user.avatar}
            icon={<UserOutlined />}
          >
            {user.username.charAt(0).toUpperCase()}
          </Avatar>
          <div>
            <div style={{ fontWeight: 500 }}>{user.username}</div>
            <div style={{ fontSize: 12, color: '#666', display: 'flex', alignItems: 'center', gap: 4 }}>
              <MailOutlined style={{ fontSize: 12 }} />
              {user.email}
            </div>
          </div>
        </div>
      ),
    },
    {
      title: '角色',
      key: 'role',
      render: (_, user) => (
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <SecurityScanOutlined style={{ color: '#666' }} />
          <Tag color={user.role === 'ADMIN' ? 'blue' : 'default'}>
            {user.role === 'ADMIN' ? '管理员' : '普通用户'}
          </Tag>
        </div>
      ),
    },
    {
      title: '状态',
      key: 'status',
      render: (_, user) => (
        <Tag color={user.active ? 'success' : 'error'}>
          {user.active ? '活跃' : '已停用'}
        </Tag>
      ),
    },
    {
      title: '创建时间',
      key: 'createdAt',
      render: (_, user) => (
        <div style={{ fontSize: 12, color: '#666', display: 'flex', alignItems: 'center', gap: 4 }}>
          <CalendarOutlined style={{ fontSize: 12 }} />
          {formatDate(user.createdAt)}
        </div>
      ),
    },
    {
      title: '最后登录',
      key: 'lastLogin',
      render: (_, user) => (
        user.lastLoginAt ? (
          <div style={{ fontSize: 12, color: '#666' }}>
            {formatDate(user.lastLoginAt)}
          </div>
        ) : (
          <div style={{ fontSize: 12, color: '#999' }}>从未登录</div>
        )
      ),
    },
    {
      title: '操作',
      key: 'actions',
      render: (_, user) => (
        <Space size="small">
          <Button
            size="small"
            icon={<EditOutlined />}
            onClick={() => handleEdit(user)}
          />
          <Button
            size="small"
            type={user.active ? 'default' : 'primary'}
            icon={user.active ? <CloseOutlined /> : <CheckOutlined />}
            onClick={() => handleToggleActive(user.id, user.active)}
          >
            {user.active ? '停用' : '激活'}
          </Button>
          <Popconfirm
            title="确定要删除此用户吗？"
            onConfirm={() => handleDelete(user.id)}
            okText="确定"
            cancelText="取消"
          >
            <Button
              size="small"
              danger
              icon={<DeleteOutlined />}
            />
          </Popconfirm>
        </Space>
      ),
    },
  ]

  if (loading) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: 400 }}>
        <Spin size="large" />
      </div>
    )
  }

  return (
    <div style={{ padding: 24 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 24 }}>
        <div>
          <Title level={2} style={{ margin: 0 }}>用户管理</Title>
          <p style={{ color: '#666', margin: '8px 0 0 0' }}>管理系统用户和权限</p>
        </div>
        <Button 
          type="primary"
          icon={<UserAddOutlined />}
          onClick={() => setShowAddModal(true)}
        >
          添加用户
        </Button>
      </div>

      {error && (
        <Alert
          message={error}
          type="error"
          closable
          onClose={() => setError(null)}
          style={{ marginBottom: 16 }}
        />
      )}

      <Card>
        <Table
          columns={columns}
          dataSource={users}
          rowKey="id"
          pagination={{
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total, range) => `第 ${range[0]}-${range[1]} 条，共 ${total} 条`
          }}
          locale={{
            emptyText: '暂无用户数据'
          }}
        />
      </Card>

      {/* 添加用户弹窗 */}
      <Modal
        title={
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <UserAddOutlined />
            添加用户
          </div>
        }
        open={showAddModal}
        onCancel={() => {
          setShowAddModal(false)
          form.resetFields()
        }}
        footer={null}
        destroyOnClose
      >
        <Form
          form={form}
          layout="vertical"
          onFinish={handleSubmit}
          style={{ marginTop: 16 }}
        >
          <Form.Item
            name="username"
            label="用户名"
            rules={[{ required: true, message: '请输入用户名' }]}
          >
            <Input placeholder="请输入用户名" />
          </Form.Item>
          
          <Form.Item
            name="email"
            label="邮箱"
            rules={[
              { required: true, message: '请输入邮箱' },
              { type: 'email', message: '请输入有效的邮箱地址' }
            ]}
          >
            <Input placeholder="请输入邮箱" />
          </Form.Item>
          
          <Form.Item
            name="password"
            label="密码"
            rules={[{ required: true, message: '请输入密码' }]}
          >
            <Input.Password placeholder="请输入密码" />
          </Form.Item>
          
          <Form.Item
            name="role"
            label="角色"
            initialValue="USER"
            rules={[{ required: true, message: '请选择角色' }]}
          >
            <Select placeholder="请选择角色">
              <Option value="USER">普通用户</Option>
              <Option value="ADMIN">管理员</Option>
            </Select>
          </Form.Item>
          
          <Form.Item style={{ marginBottom: 0 }}>
            <Space style={{ width: '100%', justifyContent: 'flex-end' }}>
              <Button onClick={() => {
                setShowAddModal(false)
                form.resetFields()
              }}>
                取消
              </Button>
              <Button type="primary" htmlType="submit">
                创建用户
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>

      {/* 编辑用户弹窗 */}
      <Modal
        title={
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <UserOutlined />
            编辑用户
          </div>
        }
        open={showEditModal}
        onCancel={() => {
          setShowEditModal(false)
          setEditingUser(null)
          form.resetFields()
        }}
        footer={null}
        destroyOnClose
      >
        <Form
          form={form}
          layout="vertical"
          onFinish={handleSubmit}
          style={{ marginTop: 16 }}
        >
          <Form.Item
            name="username"
            label="用户名"
            rules={[{ required: true, message: '请输入用户名' }]}
          >
            <Input placeholder="请输入用户名" />
          </Form.Item>
          
          <Form.Item
            name="email"
            label="邮箱"
            rules={[
              { required: true, message: '请输入邮箱' },
              { type: 'email', message: '请输入有效的邮箱地址' }
            ]}
          >
            <Input placeholder="请输入邮箱" />
          </Form.Item>
          
          <Form.Item
            name="password"
            label="密码"
            extra="留空则不修改密码"
          >
            <Input.Password placeholder="留空则不修改密码" />
          </Form.Item>
          
          <Form.Item
            name="role"
            label="角色"
            rules={[{ required: true, message: '请选择角色' }]}
          >
            <Select placeholder="请选择角色">
              <Option value="USER">普通用户</Option>
              <Option value="ADMIN">管理员</Option>
            </Select>
          </Form.Item>
          
          <Form.Item style={{ marginBottom: 0 }}>
            <Space style={{ width: '100%', justifyContent: 'flex-end' }}>
              <Button onClick={() => {
                setShowEditModal(false)
                setEditingUser(null)
                form.resetFields()
              }}>
                取消
              </Button>
              <Button type="primary" htmlType="submit">
                更新用户
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

export default UserManagement

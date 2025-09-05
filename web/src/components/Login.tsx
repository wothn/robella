import React, { useState } from 'react'
import { Card, Button, Input, Form, Alert, Space, Typography, Divider } from 'antd'
import { LockOutlined, UserOutlined, EyeInvisibleOutlined, EyeTwoTone, GithubOutlined } from '@ant-design/icons'
import { apiClient, type LoginResponse } from '@/lib/api'

const { Title, Text } = Typography

interface LoginFormData {
  username: string
  password: string
}

interface LoginProps {
  onLoginSuccess?: () => void
}

const Login: React.FC<LoginProps> = ({ onLoginSuccess }) => {
  const [form] = Form.useForm()
  const [isLoading, setIsLoading] = useState(false)
  const [isGithubLoading, setIsGithubLoading] = useState(false)
  const [error, setError] = useState('')

  const handleSubmit = async (values: LoginFormData) => {
    setIsLoading(true)
    setError('')

    try {
      const response = await apiClient.post<LoginResponse>('/users/login', values)

      if (response.accessToken && response.user) {
        // 存储认证信息
        localStorage.setItem('accessToken', response.accessToken)
        localStorage.setItem('user', JSON.stringify(response.user))
        
        // 调用登录成功回调
        if (onLoginSuccess) {
          onLoginSuccess()
        }
      } else {
        setError('登录失败：未收到有效的认证令牌')
      }
    } catch (err) {
      console.error('登录错误:', err)
      const msg = err instanceof Error ? err.message : '登录失败，请稍后重试'
      // 透传具体错误信息（包含HTTP状态码与后端消息片段）
      setError(msg)
    } finally {
      setIsLoading(false)
    }
  }

  const handleGithubLogin = () => {
    setIsGithubLoading(true)
    setError('')
    
    // 直接跳转到后端的 GitHub 登录端点，让后端处理重定向到 GitHub
  const origin = window.location.origin
  const url = `/api/auth/github/login?frontRedirect=${encodeURIComponent(origin)}`
  window.location.href = url
  }

  const handleDemoLogin = () => {
    form.setFieldsValue({
      username: 'admin',
      password: 'password'
    })
  }

  return (
    <div style={{ 
      minHeight: '100vh', 
      display: 'flex', 
      alignItems: 'center', 
      justifyContent: 'center',
      background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)'
    }}>
      <Card style={{ width: 400, boxShadow: '0 8px 32px rgba(0,0,0,0.1)' }}>
        <div style={{ textAlign: 'center', marginBottom: 24 }}>
          <Title level={2} style={{ margin: 0, color: '#1890ff' }}>
            Robella 管理系统
          </Title>
          <Text type="secondary">欢迎回来，请登录您的账户</Text>
        </div>

        {error && (
          <Alert
            message={error}
            type="error"
            closable
            onClose={() => setError('')}
            style={{ marginBottom: 16 }}
          />
        )}

        <Form
          form={form}
          layout="vertical"
          onFinish={handleSubmit}
          autoComplete="off"
        >
          <Form.Item
            name="username"
            label="用户名"
            rules={[{ required: true, message: '请输入用户名' }]}
          >
            <Input 
              prefix={<UserOutlined />}
              placeholder="请输入用户名"
              size="large"
            />
          </Form.Item>

          <Form.Item
            name="password"
            label="密码"
            rules={[{ required: true, message: '请输入密码' }]}
          >
            <Input.Password
              prefix={<LockOutlined />}
              placeholder="请输入密码"
              size="large"
              iconRender={(visible) => (visible ? <EyeTwoTone /> : <EyeInvisibleOutlined />)}
            />
          </Form.Item>

          <Form.Item>
            <Button 
              type="primary" 
              htmlType="submit" 
              loading={isLoading}
              size="large"
              block
            >
              {isLoading ? '登录中...' : '登录'}
            </Button>
          </Form.Item>
        </Form>

        <Divider>或</Divider>

        <Space direction="vertical" style={{ width: '100%' }} size="middle">
          <Button
            icon={<GithubOutlined />}
            onClick={handleGithubLogin}
            loading={isGithubLoading}
            size="large"
            block
          >
            {isGithubLoading ? '跳转中...' : '使用 GitHub 登录'}
          </Button>

          <Button
            type="dashed"
            onClick={handleDemoLogin}
            size="large"
            block
          >
            使用演示账户 (admin/password)
          </Button>
        </Space>

        <div style={{ textAlign: 'center', marginTop: 24 }}>
          <Text type="secondary" style={{ fontSize: 12 }}>
            © 2024 Robella. 保留所有权利.
          </Text>
        </div>
      </Card>
    </div>
  )
}

export default Login

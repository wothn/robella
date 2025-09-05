import React, { useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { Card, Spin, Result } from 'antd'
import { CheckCircleOutlined, CloseCircleOutlined } from '@ant-design/icons'

interface AuthCallbackProps {
  type: 'success' | 'error'
}

const AuthCallback: React.FC<AuthCallbackProps> = ({ type }) => {
  const navigate = useNavigate()

  useEffect(() => {
    const urlParams = new URLSearchParams(window.location.search)
    const token = urlParams.get('token')
    const user = urlParams.get('user')
    const error = urlParams.get('error')

    if (type === 'success' && token && user) {
      // 存储用户信息
      localStorage.setItem('user', JSON.stringify({ username: user }))
      localStorage.setItem('accessToken', token)
      
      // 延迟跳转到主页面
      const timer = setTimeout(() => {
        navigate('/')
        window.location.reload() // 强制刷新以触发登录状态更新
      }, 2000)
      
      return () => clearTimeout(timer)
    } else if (type === 'error' || error) {
      // 处理错误情况，可以选择显示错误信息
      console.error('OAuth认证失败:', error)
    }
  }, [type, navigate])

  if (type === 'success') {
    return (
      <div style={{ 
        minHeight: '100vh', 
        display: 'flex', 
        alignItems: 'center', 
        justifyContent: 'center',
        background: 'linear-gradient(135deg, #f0f9ff 0%, #e0e7ff 100%)',
        padding: 16
      }}>
        <Card style={{ 
          width: '100%', 
          maxWidth: 400, 
          textAlign: 'center',
          boxShadow: '0 8px 32px rgba(0,0,0,0.1)'
        }}>
          <Result
            icon={<CheckCircleOutlined style={{ color: '#52c41a' }} />}
            title="登录成功！"
            subTitle="正在跳转到主页面..."
            extra={<Spin size="large" />}
          />
        </Card>
      </div>
    )
  }

  return (
    <div style={{ 
      minHeight: '100vh', 
      display: 'flex', 
      alignItems: 'center', 
      justifyContent: 'center',
      background: 'linear-gradient(135deg, #fff1f0 0%, #ffe7e7 100%)',
      padding: 16
    }}>
      <Card style={{ 
        width: '100%', 
        maxWidth: 400, 
        textAlign: 'center',
        boxShadow: '0 8px 32px rgba(0,0,0,0.1)'
      }}>
        <Result
          status="error"
          icon={<CloseCircleOutlined style={{ color: '#ff4d4f' }} />}
          title="登录失败"
          subTitle="认证过程中出现了问题，请重试"
        />
      </Card>
    </div>
  )
}

export default AuthCallback

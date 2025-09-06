import React from 'react'
import { Alert, Button } from 'antd'
import { ReloadOutlined } from '@ant-design/icons'

interface ErrorBoundaryState {
  hasError: boolean
  error: Error | null
  errorInfo: React.ErrorInfo | null
}

interface ErrorBoundaryProps {
  children: React.ReactNode
  fallback?: React.ReactNode
}

class ErrorBoundary extends React.Component<ErrorBoundaryProps, ErrorBoundaryState> {
  constructor(props: ErrorBoundaryProps) {
    super(props)
    this.state = {
      hasError: false,
      error: null,
      errorInfo: null
    }
  }

  static getDerivedStateFromError(error: Error): ErrorBoundaryState {
    return {
      hasError: true,
      error,
      errorInfo: null
    }
  }

  componentDidCatch(error: Error, errorInfo: React.ErrorInfo) {
    console.error('Error caught by boundary:', error, errorInfo)
    this.setState({
      error,
      errorInfo
    })
  }

  handleRetry = () => {
    this.setState({
      hasError: false,
      error: null,
      errorInfo: null
    })
  }

  render() {
    if (this.state.hasError) {
      if (this.props.fallback) {
        return this.props.fallback
      }

      return (
        <div style={{ padding: '24px' }}>
          <Alert
            message="组件出错"
            description={
              <div>
                <p>抱歉，组件出现了问题。</p>
                {this.state.error && (
                  <p style={{ color: '#666', fontSize: '12px', marginTop: '8px' }}>
                    错误信息: {this.state.error.message}
                  </p>
                )}
                {process.env.NODE_ENV === 'development' && this.state.errorInfo && (
                  <details style={{ marginTop: '8px' }}>
                    <summary style={{ cursor: 'pointer', color: '#1890ff' }}>
                      错误详情
                    </summary>
                    <pre style={{ 
                      fontSize: '11px', 
                      background: '#f5f5f5', 
                      padding: '8px', 
                      borderRadius: '4px',
                      marginTop: '8px',
                      overflow: 'auto',
                      maxHeight: '200px'
                    }}>
                      {this.state.errorInfo.componentStack}
                    </pre>
                  </details>
                )}
              </div>
            }
            type="error"
            action={
              <Button 
                icon={<ReloadOutlined />} 
                onClick={this.handleRetry}
                type="primary"
                size="small"
              >
                重试
              </Button>
            }
            showIcon
          />
        </div>
      )
    }

    return this.props.children
  }
}

export default ErrorBoundary
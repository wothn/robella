import React from 'react'
import { Spin } from 'antd'

interface LoadingProps {
  size?: 'small' | 'default' | 'large'
  tip?: string
  delay?: number
  children?: React.ReactNode
}

const Loading: React.FC<LoadingProps> = ({ 
  size = 'default', 
  tip = '加载中...', 
  delay = 300,
  children 
}) => {
  return (
    <div style={{ 
      display: 'flex', 
      justifyContent: 'center', 
      alignItems: 'center', 
      minHeight: '200px',
      width: '100%'
    }}>
      <Spin size={size} tip={tip} delay={delay}>
        {children}
      </Spin>
    </div>
  )
}

export default Loading
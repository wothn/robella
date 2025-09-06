import React, { useEffect, useCallback, useState } from 'react'
import { 
  Layout, 
  Card, 
  Button, 
  Table, 
  Input, 
  Switch, 
  Modal, 
  Form, 
  Select, 
  Tag, 
  Space, 
  Alert,
  Row,
  Col,
  Typography,
  Divider,
  message,
  Popconfirm
} from 'antd'
import { 
  PlusOutlined, 
  EditOutlined, 
  DeleteOutlined, 
  ReloadOutlined,
  SettingOutlined,
  ApiOutlined
} from '@ant-design/icons'
import { apiClient } from '../lib/api'
import type { Provider, Model } from '../types'
import { ErrorBoundary, Loading } from './common'
import { useAppContext } from '../contexts/AppContext'

const { Sider, Content } = Layout
const { Title, Text } = Typography
const { Search } = Input
const { Option } = Select

interface EditableProvider extends Provider {
  _editing?: boolean
  _loading?: boolean
}

interface EditableModel extends Model {
  _editing?: boolean
  _loading?: boolean
}

const ProviderModelManager: React.FC = () => {
  const { state, dispatch } = useAppContext()
  const [providerForm] = Form.useForm()
  const [modelForm] = Form.useForm()
  const [providerModalVisible, setProviderModalVisible] = useState(false)
  const [modelModalVisible, setModelModalVisible] = useState(false)
  const [editingProvider, setEditingProvider] = useState<Provider | null>(null)
  const [editingModel, setEditingModel] = useState<Model | null>(null)
  const [providerSearch, setProviderSearch] = useState('')
  const [testingProvider, setTestingProvider] = useState(false)

  const loadProviders = useCallback(async () => {
    try {
      dispatch({ type: 'SET_LOADING', payload: true })
      const providers = await apiClient.getProviders()
      dispatch({ type: 'SET_PROVIDERS', payload: providers })
      
      if (providers.length > 0 && !state.currentProviderId) {
        dispatch({ type: 'SET_CURRENT_PROVIDER', payload: providers[0].id })
      }
    } catch (error) {
      dispatch({ type: 'SET_ERROR', payload: error instanceof Error ? error.message : '加载提供商失败' })
    } finally {
      dispatch({ type: 'SET_LOADING', payload: false })
    }
  }, [dispatch, state.currentProviderId])

  const loadModels = useCallback(async (providerId: number) => {
    try {
      const models = await apiClient.getModelsByProvider(providerId)
      dispatch({ type: 'SET_MODELS', payload: { providerId, models } })
    } catch (error) {
      console.error('Failed to load models:', error)
    }
  }, [dispatch])

  useEffect(() => {
    loadProviders()
  }, [loadProviders])

  useEffect(() => {
    if (state.currentProviderId) {
      loadModels(state.currentProviderId)
    }
  }, [state.currentProviderId, loadModels])

  const filteredProviders = state.providers.filter(provider => {
    if (!providerSearch.trim()) return true
    const search = providerSearch.trim().toLowerCase()
    return (
      provider.name.toLowerCase().includes(search) ||
      (provider.type || '').toLowerCase().includes(search)
    )
  })

  const currentProvider = state.providers.find(p => p.id === state.currentProviderId)
  const currentModels = state.currentProviderId ? (state.models[state.currentProviderId] || []) : []

  const handleProviderSubmit = async (values: any) => {
    try {
      if (editingProvider) {
        const updated = await apiClient.patchProvider(editingProvider.id, values)
        dispatch({ type: 'UPDATE_PROVIDER', payload: updated })
        message.success('提供商更新成功')
      } else {
        const created = await apiClient.createProvider(values)
        dispatch({ type: 'ADD_PROVIDER', payload: created })
        message.success('提供商创建成功')
      }
      
      setProviderModalVisible(false)
      setEditingProvider(null)
      providerForm.resetFields()
    } catch (error) {
      message.error(error instanceof Error ? error.message : '操作失败')
    }
  }

  const handleModelSubmit = async (values: any) => {
    if (!state.currentProviderId) return
    
    try {
      if (editingModel) {
        const updated = await apiClient.patchModel(editingModel.id, values)
        dispatch({ type: 'UPDATE_MODEL', payload: updated })
        message.success('模型更新成功')
      } else {
        const created = await apiClient.createModel(state.currentProviderId, values)
        dispatch({ type: 'ADD_MODEL', payload: { providerId: state.currentProviderId, model: created } })
        message.success('模型创建成功')
      }
      
      setModelModalVisible(false)
      setEditingModel(null)
      modelForm.resetFields()
    } catch (error) {
      message.error(error instanceof Error ? error.message : '操作失败')
    }
  }

  const handleDeleteProvider = async (providerId: number) => {
    try {
      await apiClient.deleteProvider(providerId)
      dispatch({ type: 'DELETE_PROVIDER', payload: providerId })
      message.success('提供商删除成功')
    } catch (error) {
      message.error(error instanceof Error ? error.message : '删除失败')
    }
  }

  const handleDeleteModel = async (modelId: number) => {
    try {
      await apiClient.deleteModel(modelId)
      dispatch({ type: 'DELETE_MODEL', payload: modelId })
      message.success('模型删除成功')
    } catch (error) {
      message.error(error instanceof Error ? error.message : '删除失败')
    }
  }

  const handleToggleProvider = async (provider: Provider) => {
    try {
      const updated = await apiClient.patchProvider(provider.id, { enabled: !provider.enabled })
      dispatch({ type: 'UPDATE_PROVIDER', payload: updated })
      message.success(`提供商${updated.enabled ? '启用' : '禁用'}成功`)
    } catch (error) {
      message.error(error instanceof Error ? error.message : '操作失败')
    }
  }

  const handleToggleModel = async (model: Model) => {
    try {
      const updated = await apiClient.patchModel(model.id, { enabled: !model.enabled })
      dispatch({ type: 'UPDATE_MODEL', payload: updated })
      message.success(`模型${updated.enabled ? '启用' : '禁用'}成功`)
    } catch (error) {
      message.error(error instanceof Error ? error.message : '操作失败')
    }
  }

  const handleTestProvider = async () => {
    if (!currentProvider) return
    
    try {
      setTestingProvider(true)
      await apiClient.getModelsByProvider(currentProvider.id)
      message.success('连接测试成功')
    } catch (error) {
      message.error(error instanceof Error ? error.message : '连接测试失败')
    } finally {
      setTestingProvider(false)
    }
  }

  const providerColumns = [
    {
      title: '名称',
      dataIndex: 'name',
      key: 'name',
      render: (text: string, record: Provider) => (
        <Space>
          <Text strong>{text}</Text>
          <Tag color="blue">{record.type || 'Unknown'}</Tag>
        </Space>
      )
    },
    {
      title: '状态',
      dataIndex: 'enabled',
      key: 'enabled',
      render: (enabled: boolean) => (
        <Tag color={enabled ? 'green' : 'red'}>
          {enabled ? '启用' : '禁用'}
        </Tag>
      )
    },
    {
      title: '操作',
      key: 'actions',
      render: (_: any, record: Provider) => (
        <Space>
          <Button
            size="small"
            icon={<EditOutlined />}
            onClick={() => {
              setEditingProvider(record)
              providerForm.setFieldsValue(record)
              setProviderModalVisible(true)
            }}
          />
          <Switch
            size="small"
            checked={record.enabled}
            onChange={() => handleToggleProvider(record)}
          />
          <Popconfirm
            title="确定要删除此提供商吗？"
            onConfirm={() => handleDeleteProvider(record.id)}
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
      )
    }
  ]

  const modelColumns = [
    {
      title: '名称',
      dataIndex: 'name',
      key: 'name',
      render: (text: string, record: Model) => (
        <Space>
          <Text strong>{text}</Text>
          {record.vendorModel && (
            <Tag color="cyan">{record.vendorModel}</Tag>
          )}
        </Space>
      )
    },
    {
      title: '描述',
      dataIndex: 'description',
      key: 'description',
      render: (text: string) => text || '-'
    },
    {
      title: '状态',
      dataIndex: 'enabled',
      key: 'enabled',
      render: (enabled: boolean) => (
        <Tag color={enabled ? 'green' : 'red'}>
          {enabled ? '启用' : '禁用'}
        </Tag>
      )
    },
    {
      title: '操作',
      key: 'actions',
      render: (_: any, record: Model) => (
        <Space>
          <Button
            size="small"
            icon={<EditOutlined />}
            onClick={() => {
              setEditingModel(record)
              modelForm.setFieldsValue(record)
              setModelModalVisible(true)
            }}
          />
          <Switch
            size="small"
            checked={record.enabled}
            onChange={() => handleToggleModel(record)}
          />
          <Popconfirm
            title="确定要删除此模型吗？"
            onConfirm={() => handleDeleteModel(record.id)}
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
      )
    }
  ]

  if (state.loading) {
    return <Loading />
  }

  return (
    <ErrorBoundary>
      <Layout style={{ minHeight: '100vh', background: '#f5f5f5' }}>
        <Sider width={300} style={{ background: '#fff', borderRight: '1px solid #f0f0f0' }}>
          <div style={{ padding: '16px' }}>
            <Title level={4} style={{ margin: 0 }}>
              <ApiOutlined /> 提供商列表
            </Title>
          </div>
          <Divider style={{ margin: 0 }} />
          <div style={{ padding: '16px' }}>
            <Search
              placeholder="搜索提供商..."
              allowClear
              onSearch={setProviderSearch}
              style={{ marginBottom: '16px' }}
            />
            <Button
              type="primary"
              icon={<PlusOutlined />}
              onClick={() => {
                setEditingProvider(null)
                providerForm.resetFields()
                setProviderModalVisible(true)
              }}
              style={{ width: '100%' }}
            >
              添加提供商
            </Button>
          </div>
          <div style={{ padding: '0 16px 16px' }}>
            <Table
              dataSource={filteredProviders}
              columns={providerColumns}
              rowKey="id"
              pagination={false}
              size="small"
              onRow={(record) => ({
                onClick: () => dispatch({ type: 'SET_CURRENT_PROVIDER', payload: record.id }),
                style: {
                  cursor: 'pointer',
                  background: record.id === state.currentProviderId ? '#e6f7ff' : 'transparent'
                }
              })}
            />
          </div>
        </Sider>
        
        <Content style={{ padding: '24px' }}>
          {state.error && (
            <Alert
              message="错误"
              description={state.error}
              type="error"
              showIcon
              closable
              onClose={() => dispatch({ type: 'SET_ERROR', payload: null })}
              style={{ marginBottom: '24px' }}
            />
          )}

          {currentProvider ? (
            <>
              <Card 
                title={
                  <Space>
                    <SettingOutlined />
                    {currentProvider.name}
                    <Tag color="blue">{currentProvider.type}</Tag>
                  </Space>
                }
                extra={
                  <Space>
                    <Button
                      icon={<ReloadOutlined />}
                      onClick={handleTestProvider}
                      loading={testingProvider}
                    >
                      测试连接
                    </Button>
                    <Button
                      icon={<EditOutlined />}
                      onClick={() => {
                        setEditingProvider(currentProvider)
                        providerForm.setFieldsValue(currentProvider)
                        setProviderModalVisible(true)
                      }}
                    >
                      编辑
                    </Button>
                    <Switch
                      checked={currentProvider.enabled}
                      onChange={() => handleToggleProvider(currentProvider)}
                    />
                  </Space>
                }
                style={{ marginBottom: '24px' }}
              >
                <Row gutter={16}>
                  <Col span={12}>
                    <Text type="secondary">API Key:</Text>
                    <div style={{ marginTop: 4 }}>
                      <Text code>
                        {currentProvider.apiKey ? 
                          `${currentProvider.apiKey.substring(0, 8)}...${currentProvider.apiKey.slice(-4)}` : 
                          '未设置'
                        }
                      </Text>
                    </div>
                  </Col>
                  <Col span={12}>
                    <Text type="secondary">Base URL:</Text>
                    <div style={{ marginTop: 4 }}>
                      <Text code>{currentProvider.baseUrl || '未设置'}</Text>
                    </div>
                  </Col>
                  {currentProvider.deploymentName && (
                    <Col span={12}>
                      <Text type="secondary">Deployment:</Text>
                      <div style={{ marginTop: 4 }}>
                        <Text code>{currentProvider.deploymentName}</Text>
                      </div>
                    </Col>
                  )}
                  {currentProvider.apiVersion && (
                    <Col span={12}>
                      <Text type="secondary">API Version:</Text>
                      <div style={{ marginTop: 4 }}>
                        <Text code>{currentProvider.apiVersion}</Text>
                      </div>
                    </Col>
                  )}
                </Row>
              </Card>

              <Card
                title={
                  <Space>
                    <PlusOutlined />
                    模型管理
                  </Space>
                }
                extra={
                  <Button
                    type="primary"
                    icon={<PlusOutlined />}
                    onClick={() => {
                      setEditingModel(null)
                      modelForm.resetFields()
                      setModelModalVisible(true)
                    }}
                  >
                    添加模型
                  </Button>
                }
              >
                <Table
                  dataSource={currentModels}
                  columns={modelColumns}
                  rowKey="id"
                  pagination={{
                    showSizeChanger: true,
                    showQuickJumper: true,
                    showTotal: (total, range) => 
                      `第 ${range[0]}-${range[1]} 条，共 ${total} 条`
                  }}
                  locale={{ emptyText: '暂无模型数据' }}
                />
              </Card>
            </>
          ) : (
            <div style={{ textAlign: 'center', padding: '100px 0' }}>
              <Text type="secondary">请从左侧选择一个提供商</Text>
            </div>
          )}
        </Content>
      </Layout>

      {/* Provider Modal */}
      <Modal
        title={editingProvider ? '编辑提供商' : '添加提供商'}
        open={providerModalVisible}
        onCancel={() => {
          setProviderModalVisible(false)
          setEditingProvider(null)
          providerForm.resetFields()
        }}
        footer={null}
        destroyOnClose
      >
        <Form
          form={providerForm}
          layout="vertical"
          onFinish={handleProviderSubmit}
        >
          <Form.Item
            name="name"
            label="名称"
            rules={[{ required: true, message: '请输入提供商名称' }]}
          >
            <Input placeholder="请输入提供商名称" />
          </Form.Item>
          
          <Form.Item
            name="type"
            label="类型"
            rules={[{ required: true, message: '请选择提供商类型' }]}
          >
            <Select placeholder="请选择提供商类型">
              <Option value="OpenAI">OpenAI</Option>
              <Option value="Anthropic">Anthropic</Option>
              <Option value="Azure">Azure</Option>
              <Option value="Other">Other</Option>
            </Select>
          </Form.Item>
          
          <Form.Item
            name="apiKey"
            label="API Key"
            rules={[{ required: true, message: '请输入API Key' }]}
          >
            <Input.Password placeholder="请输入API Key" />
          </Form.Item>
          
          <Form.Item
            name="baseUrl"
            label="Base URL"
          >
            <Input placeholder="请输入Base URL" />
          </Form.Item>
          
          <Form.Item
            name="deploymentName"
            label="Deployment Name"
          >
            <Input placeholder="请输入Deployment Name" />
          </Form.Item>
          
          <Form.Item
            name="apiVersion"
            label="API Version"
          >
            <Input placeholder="请输入API Version" />
          </Form.Item>
          
          <Form.Item
            name="enabled"
            label="启用状态"
            valuePropName="checked"
            initialValue={true}
          >
            <Switch />
          </Form.Item>
          
          <Form.Item style={{ marginBottom: 0 }}>
            <Space style={{ width: '100%', justifyContent: 'flex-end' }}>
              <Button onClick={() => {
                setProviderModalVisible(false)
                setEditingProvider(null)
                providerForm.resetFields()
              }}>
                取消
              </Button>
              <Button type="primary" htmlType="submit">
                {editingProvider ? '更新' : '创建'}
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>

      {/* Model Modal */}
      <Modal
        title={editingModel ? '编辑模型' : '添加模型'}
        open={modelModalVisible}
        onCancel={() => {
          setModelModalVisible(false)
          setEditingModel(null)
          modelForm.resetFields()
        }}
        footer={null}
        destroyOnClose
      >
        <Form
          form={modelForm}
          layout="vertical"
          onFinish={handleModelSubmit}
        >
          <Form.Item
            name="name"
            label="名称"
            rules={[{ required: true, message: '请输入模型名称' }]}
          >
            <Input placeholder="请输入模型名称" />
          </Form.Item>
          
          <Form.Item
            name="vendorModel"
            label="Vendor Model"
          >
            <Input placeholder="请输入Vendor Model" />
          </Form.Item>
          
          <Form.Item
            name="description"
            label="描述"
          >
            <Input.TextArea placeholder="请输入模型描述" rows={3} />
          </Form.Item>
          
          <Form.Item
            name="enabled"
            label="启用状态"
            valuePropName="checked"
            initialValue={true}
          >
            <Switch />
          </Form.Item>
          
          <Form.Item style={{ marginBottom: 0 }}>
            <Space style={{ width: '100%', justifyContent: 'flex-end' }}>
              <Button onClick={() => {
                setModelModalVisible(false)
                setEditingModel(null)
                modelForm.resetFields()
              }}>
                取消
              </Button>
              <Button type="primary" htmlType="submit">
                {editingModel ? '更新' : '创建'}
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>
    </ErrorBoundary>
  )
}

export default ProviderModelManager
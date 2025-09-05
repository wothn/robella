import React, { useState, useEffect } from 'react';
import {
  Table,
  Button,
  Modal,
  Form,
  Input,
  Select,
  Switch,
  Space,
  message,
  Popconfirm,
  Card,
  Row,
  Col,
  Tag,
  Tabs
} from 'antd';
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  SettingOutlined
} from '@ant-design/icons';

const { Option } = Select;
const { TabPane } = Tabs;

interface Provider {
  id: number;
  name: string;
  type: string;
  apiKey: string;
  baseUrl: string;
  deploymentName?: string;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

interface Model {
  id: number;
  providerId: number;
  name: string;
  vendorModel: string;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

const ProviderManagement: React.FC = () => {
  const [providers, setProviders] = useState<Provider[]>([]);
  const [models, setModels] = useState<Model[]>([]);
  const [loading, setLoading] = useState(false);
  const [providerModalVisible, setProviderModalVisible] = useState(false);
  const [modelModalVisible, setModelModalVisible] = useState(false);
  const [editingProvider, setEditingProvider] = useState<Provider | null>(null);
  const [editingModel, setEditingModel] = useState<Model | null>(null);
  const [selectedProvider, setSelectedProvider] = useState<Provider | null>(null);
  const [form] = Form.useForm();
  const [modelForm] = Form.useForm();

  useEffect(() => {
    fetchProviders();
  }, []);

  const fetchProviders = async () => {
    setLoading(true);
    try {
      const response = await fetch('/api/providers');
      const data = await response.json();
      setProviders(data);
    } catch (error) {
      message.error('获取提供商列表失败');
    } finally {
      setLoading(false);
    }
  };

  const fetchModels = async (providerId: number) => {
    try {
      const response = await fetch(`/api/providers/${providerId}/models`);
      const data = await response.json();
      setModels(data);
    } catch (error) {
      message.error('获取模型列表失败');
    }
  };

  const handleProviderSubmit = async (values: any) => {
    try {
      const url = editingProvider ? `/api/providers/${editingProvider.id}` : '/api/providers';
      const method = editingProvider ? 'PUT' : 'POST';
      
      const response = await fetch(url, {
        method,
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(values),
      });

      if (response.ok) {
        message.success(editingProvider ? '更新成功' : '创建成功');
        setProviderModalVisible(false);
        form.resetFields();
        setEditingProvider(null);
        fetchProviders();
      } else {
        message.error('操作失败');
      }
    } catch (error) {
      message.error('操作失败');
    }
  };

  const handleModelSubmit = async (values: any) => {
    if (!selectedProvider) return;
    
    try {
      const url = editingModel ? `/api/providers/models/${editingModel.id}` : `/api/providers/${selectedProvider.id}/models`;
      const method = editingModel ? 'PUT' : 'POST';
      
      const response = await fetch(url, {
        method,
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(values),
      });

      if (response.ok) {
        message.success(editingModel ? '更新成功' : '创建成功');
        setModelModalVisible(false);
        modelForm.resetFields();
        setEditingModel(null);
        if (selectedProvider) {
          fetchModels(selectedProvider.id);
        }
      } else {
        message.error('操作失败');
      }
    } catch (error) {
      message.error('操作失败');
    }
  };

  const deleteProvider = async (id: number) => {
    try {
      const response = await fetch(`/api/providers/${id}`, {
        method: 'DELETE',
      });

      if (response.ok) {
        message.success('删除成功');
        fetchProviders();
      } else {
        message.error('删除失败');
      }
    } catch (error) {
      message.error('删除失败');
    }
  };

  const deleteModel = async (id: number) => {
    try {
      const response = await fetch(`/api/providers/models/${id}`, {
        method: 'DELETE',
      });

      if (response.ok) {
        message.success('删除成功');
        if (selectedProvider) {
          fetchModels(selectedProvider.id);
        }
      } else {
        message.error('删除失败');
      }
    } catch (error) {
      message.error('删除失败');
    }
  };

  const editProvider = (provider: Provider) => {
    setEditingProvider(provider);
    form.setFieldsValue(provider);
    setProviderModalVisible(true);
  };

  const editModel = (model: Model) => {
    setEditingModel(model);
    modelForm.setFieldsValue(model);
    setModelModalVisible(true);
  };

  const showModelModal = (provider: Provider) => {
    setSelectedProvider(provider);
    setEditingModel(null);
    modelForm.resetFields();
    setModelModalVisible(true);
  };

  const providerColumns = [
    {
      title: '名称',
      dataIndex: 'name',
      key: 'name',
    },
    {
      title: '类型',
      dataIndex: 'type',
      key: 'type',
      render: (type: string) => (
        <Tag color={type === 'OpenAI' ? 'green' : 'blue'}>
          {type}
        </Tag>
      ),
    },
    {
      title: '基础URL',
      dataIndex: 'baseUrl',
      key: 'baseUrl',
      ellipsis: true,
    },
    {
      title: '部署名称',
      dataIndex: 'deploymentName',
      key: 'deploymentName',
    },
    {
      title: '状态',
      dataIndex: 'active',
      key: 'active',
      render: (active: boolean) => (
        <Tag color={active ? 'green' : 'red'}>
          {active ? '启用' : '禁用'}
        </Tag>
      ),
    },
    {
      title: '操作',
      key: 'action',
      render: (_, record: Provider) => (
        <Space size="middle">
          <Button
            type="link"
            icon={<EditOutlined />}
            onClick={() => editProvider(record)}
          >
            编辑
          </Button>
          <Button
            type="link"
            icon={<SettingOutlined />}
            onClick={() => {
              setSelectedProvider(record);
              fetchModels(record.id);
            }}
          >
            模型管理
          </Button>
          <Popconfirm
            title="确定删除这个提供商吗？"
            onConfirm={() => deleteProvider(record.id)}
            okText="确定"
            cancelText="取消"
          >
            <Button
              type="link"
              danger
              icon={<DeleteOutlined />}
            >
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  const modelColumns = [
    {
      title: '模型名称',
      dataIndex: 'name',
      key: 'name',
    },
    {
      title: '供应商模型',
      dataIndex: 'vendorModel',
      key: 'vendorModel',
    },
    {
      title: '状态',
      dataIndex: 'active',
      key: 'active',
      render: (active: boolean) => (
        <Tag color={active ? 'green' : 'red'}>
          {active ? '启用' : '禁用'}
        </Tag>
      ),
    },
    {
      title: '操作',
      key: 'action',
      render: (_, record: Model) => (
        <Space size="middle">
          <Button
            type="link"
            icon={<EditOutlined />}
            onClick={() => editModel(record)}
          >
            编辑
          </Button>
          <Popconfirm
            title="确定删除这个模型吗？"
            onConfirm={() => deleteModel(record.id)}
            okText="确定"
            cancelText="取消"
          >
            <Button
              type="link"
              danger
              icon={<DeleteOutlined />}
            >
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div style={{ padding: '24px' }}>
      <Card>
        <Row justify="space-between" align="middle" style={{ marginBottom: '16px' }}>
          <Col>
            <h2>AI提供商管理</h2>
          </Col>
          <Col>
            <Button
              type="primary"
              icon={<PlusOutlined />}
              onClick={() => {
                setEditingProvider(null);
                form.resetFields();
                setProviderModalVisible(true);
              }}
            >
              添加提供商
            </Button>
          </Col>
        </Row>

        <Table
          columns={providerColumns}
          dataSource={providers}
          rowKey="id"
          loading={loading}
          pagination={{
            pageSize: 10,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total, range) =>
              `第 ${range[0]}-${range[1]} 条，共 ${total} 条`,
          }}
        />
      </Card>

      {selectedProvider && (
        <Card style={{ marginTop: '24px' }}>
          <Row justify="space-between" align="middle" style={{ marginBottom: '16px' }}>
            <Col>
              <h3>{selectedProvider.name} - 模型管理</h3>
            </Col>
            <Col>
              <Button
                type="primary"
                icon={<PlusOutlined />}
                onClick={() => showModelModal(selectedProvider)}
              >
                添加模型
              </Button>
            </Col>
          </Row>

          <Table
            columns={modelColumns}
            dataSource={models}
            rowKey="id"
            pagination={{
              pageSize: 10,
              showSizeChanger: true,
              showQuickJumper: true,
              showTotal: (total, range) =>
                `第 ${range[0]}-${range[1]} 条，共 ${total} 条`,
            }}
          />
        </Card>
      )}

      <Modal
        title={editingProvider ? '编辑提供商' : '添加提供商'}
        open={providerModalVisible}
        onCancel={() => {
          setProviderModalVisible(false);
          form.resetFields();
          setEditingProvider(null);
        }}
        footer={null}
      >
        <Form
          form={form}
          layout="vertical"
          onFinish={handleProviderSubmit}
        >
          <Form.Item
            name="name"
            label="名称"
            rules={[{ required: true, message: '请输入名称' }]}
          >
            <Input placeholder="请输入提供商名称" />
          </Form.Item>
          
          <Form.Item
            name="type"
            label="类型"
            rules={[{ required: true, message: '请选择类型' }]}
          >
            <Select placeholder="请选择提供商类型">
              <Option value="OpenAI">OpenAI</Option>
              <Option value="Anthropic">Anthropic</Option>
            </Select>
          </Form.Item>
          
          <Form.Item
            name="apiKey"
            label="API密钥"
            rules={[{ required: true, message: '请输入API密钥' }]}
          >
            <Input.Password placeholder="请输入API密钥" />
          </Form.Item>
          
          <Form.Item
            name="baseUrl"
            label="基础URL"
            rules={[{ required: true, message: '请输入基础URL' }]}
          >
            <Input placeholder="请输入基础URL" />
          </Form.Item>
          
          <Form.Item
            name="deploymentName"
            label="部署名称"
          >
            <Input placeholder="请输入部署名称（可选）" />
          </Form.Item>
          
          <Form.Item
            name="active"
            label="启用状态"
            valuePropName="checked"
            initialValue={true}
          >
            <Switch />
          </Form.Item>
          
          <Form.Item>
            <Space>
              <Button type="primary" htmlType="submit">
                {editingProvider ? '更新' : '创建'}
              </Button>
              <Button
                onClick={() => {
                  setProviderModalVisible(false);
                  form.resetFields();
                  setEditingProvider(null);
                }}
              >
                取消
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title={editingModel ? '编辑模型' : '添加模型'}
        open={modelModalVisible}
        onCancel={() => {
          setModelModalVisible(false);
          modelForm.resetFields();
          setEditingModel(null);
        }}
        footer={null}
      >
        <Form
          form={modelForm}
          layout="vertical"
          onFinish={handleModelSubmit}
        >
          <Form.Item
            name="name"
            label="模型名称"
            rules={[{ required: true, message: '请输入模型名称' }]}
          >
            <Input placeholder="请输入模型名称" />
          </Form.Item>
          
          <Form.Item
            name="vendorModel"
            label="供应商模型"
            rules={[{ required: true, message: '请输入供应商模型' }]}
          >
            <Input placeholder="请输入供应商模型名称" />
          </Form.Item>
          
          <Form.Item
            name="active"
            label="启用状态"
            valuePropName="checked"
            initialValue={true}
          >
            <Switch />
          </Form.Item>
          
          <Form.Item>
            <Space>
              <Button type="primary" htmlType="submit">
                {editingModel ? '更新' : '创建'}
              </Button>
              <Button
                onClick={() => {
                  setModelModalVisible(false);
                  modelForm.resetFields();
                  setEditingModel(null);
                }}
              >
                取消
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default ProviderManagement;
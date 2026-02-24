import { useEffect, useState } from 'react';
import {
  Card, Table, Tag, Typography, Badge, Space, Spin, Alert,
  Button, Switch, Modal, Input, Select, InputNumber, Checkbox,
  Popconfirm, message, Row, Col,
} from 'antd';
import {
  RobotOutlined, PlusOutlined, EditOutlined, DeleteOutlined,
  CheckCircleOutlined, ApiOutlined,
} from '@ant-design/icons';
import { fetchAdminModels, upsertModel, deleteModel, toggleModel, fetchModels } from '../core/api/chat';
import { useChatStore } from '../core/state/chatStore';

const { Title, Text } = Typography;

const EMPTY_FORM = {
  modelId: '', displayName: '', provider: 'openai',
  baseUrl: '', apiKey: '', modelName: '',
  capabilities: { chat: true, tools: true, jsonMode: true, vision: false },
  sortOrder: 100,
};

export default function Models() {
  const { modelId, setModelId, setModels } = useChatStore();
  const [adminModels, setAdminModels] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState({ ...EMPTY_FORM });
  const [busy, setBusy] = useState(false);
  const [msgApi, contextHolder] = message.useMessage();

  const reload = async () => {
    try {
      const data = await fetchAdminModels();
      setAdminModels(data);
      const enabledModels = await fetchModels();
      setModels(enabledModels);
    } catch (e) {
      setError(e.message);
    }
  };

  useEffect(() => {
    reload().finally(() => setLoading(false));
  }, []);

  const onToggle = async (record) => {
    try {
      setBusy(true);
      await toggleModel(record.modelId);
      await reload();
    } catch (e) {
      msgApi.error(`Toggle failed: ${e.message}`);
    } finally {
      setBusy(false);
    }
  };

  const onAdd = () => {
    setEditing(null);
    setForm({ ...EMPTY_FORM });
    setModalOpen(true);
  };

  const onEdit = (record) => {
    setEditing(record);
    setForm({
      modelId: record.modelId,
      displayName: record.displayName || record.modelId,
      provider: record.provider || 'openai',
      baseUrl: '',
      apiKey: '',
      modelName: '',
      capabilities: record.capabilities || { chat: true, tools: true, jsonMode: true, vision: false },
      sortOrder: 100,
    });
    setModalOpen(true);
  };

  const onSave = async () => {
    if (!form.modelId.trim() || !form.baseUrl.trim() || !form.apiKey.trim() || !form.modelName.trim()) {
      msgApi.warning('Model ID, Base URL, API Key, Model Name are required');
      return;
    }
    try {
      setBusy(true);
      await upsertModel({
        modelId: form.modelId.trim(),
        provider: form.provider || 'openai',
        displayName: form.displayName.trim() || form.modelId.trim(),
        baseUrl: form.baseUrl.trim(),
        apiKey: form.apiKey.trim(),
        modelName: form.modelName.trim(),
        capabilities: form.capabilities,
        sortOrder: form.sortOrder,
      });
      setModalOpen(false);
      await reload();
      msgApi.success(editing ? 'Model updated' : 'Model added');
    } catch (e) {
      msgApi.error(`Save failed: ${e.message}`);
    } finally {
      setBusy(false);
    }
  };

  const onDelete = async (record) => {
    try {
      setBusy(true);
      await deleteModel(record.modelId);
      await reload();
      msgApi.success('Model deleted');
    } catch (e) {
      msgApi.error(`Delete failed: ${e.message}`);
    } finally {
      setBusy(false);
    }
  };

  const capTags = (caps) => {
    if (!caps) return null;
    const items = [];
    if (caps.chat) items.push(<Tag key="chat" color="blue" style={{ fontSize: 11, padding: '0 4px' }}>Chat</Tag>);
    if (caps.tools) items.push(<Tag key="tools" color="cyan" style={{ fontSize: 11, padding: '0 4px' }}>Tools</Tag>);
    if (caps.jsonMode) items.push(<Tag key="json" color="purple" style={{ fontSize: 11, padding: '0 4px' }}>JSON</Tag>);
    if (caps.vision) items.push(<Tag key="vision" color="orange" style={{ fontSize: 11, padding: '0 4px' }}>Vision</Tag>);
    return <Space size={2}>{items}</Space>;
  };

  const healthBadge = (h) => {
    if (h === 'UP') return <Badge status="success" text="UP" />;
    if (h === 'DOWN') return <Badge status="error" text="DOWN" />;
    return <Badge status="warning" text={h || 'UNKNOWN'} />;
  };

  const columns = [
    {
      title: 'Enabled',
      key: 'enabled',
      width: 80,
      render: (_, r) => (
        <Switch
          size="small"
          checked={r.enabled}
          loading={busy}
          onChange={() => onToggle(r)}
        />
      ),
    },
    {
      title: 'Model ID',
      dataIndex: 'modelId',
      key: 'modelId',
      render: (id) => (
        <Space>
          <RobotOutlined style={{ color: '#1677ff' }} />
          <Text strong style={{ fontSize: 13 }}>{id}</Text>
          {id === modelId && <Tag color="blue">Current</Tag>}
        </Space>
      ),
    },
    {
      title: 'Display Name',
      dataIndex: 'displayName',
      key: 'displayName',
      render: (v) => <Text style={{ fontSize: 13 }}>{v}</Text>,
    },
    {
      title: 'Provider',
      dataIndex: 'provider',
      key: 'provider',
      width: 120,
      render: (p) => <Tag color="geekblue">{p || 'Unknown'}</Tag>,
    },
    {
      title: 'Source',
      dataIndex: 'source',
      key: 'source',
      width: 90,
      render: (s) => (
        <Tag color={s === 'builtin' ? 'default' : 'green'}>{s === 'builtin' ? 'Built-in' : 'Dynamic'}</Tag>
      ),
    },
    {
      title: 'Capabilities',
      key: 'capabilities',
      render: (_, r) => capTags(r.capabilities),
    },
    {
      title: 'Health',
      dataIndex: 'health',
      key: 'health',
      width: 90,
      render: healthBadge,
    },
    {
      title: 'Actions',
      key: 'action',
      width: 180,
      render: (_, record) => (
        <Space size={4}>
          {record.modelId !== modelId && record.enabled && (
            <a onClick={() => setModelId(record.modelId)}>Use</a>
          )}
          {record.modelId === modelId && (
            <Tag color="green" icon={<CheckCircleOutlined />}>Current</Tag>
          )}
          {record.editable && (
            <a onClick={() => onEdit(record)}><EditOutlined /></a>
          )}
          {record.editable && (
            <Popconfirm
              title="Delete this model?"
              onConfirm={() => onDelete(record)}
              okText="Delete"
              cancelText="Cancel"
              okButtonProps={{ danger: true }}
            >
              <a style={{ color: '#ff4d4f' }}><DeleteOutlined /></a>
            </Popconfirm>
          )}
        </Space>
      ),
    },
  ];

  const updateForm = (key, value) => setForm((prev) => ({ ...prev, [key]: value }));
  const updateCap = (key, value) => setForm((prev) => ({
    ...prev,
    capabilities: { ...prev.capabilities, [key]: value },
  }));

  return (
    <div>
      {contextHolder}
      <div style={{ marginBottom: 24, display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
        <div>
          <Title level={4} style={{ margin: 0 }}>Model Management</Title>
          <Text type="secondary">Configure and manage AI models, add OpenAI-compatible models</Text>
        </div>
        <Button type="primary" icon={<PlusOutlined />} onClick={onAdd}>Add Model</Button>
      </div>

      {error && <Alert type="error" message={error} showIcon style={{ marginBottom: 16 }} closable onClose={() => setError('')} />}

      <Card>
        <Spin spinning={loading}>
          <Table
            dataSource={adminModels}
            columns={columns}
            rowKey="modelId"
            pagination={false}
            size="middle"
            locale={{ emptyText: loading ? 'Loading...' : 'No model data' }}
            rowClassName={(record) => record.modelId === modelId ? 'ant-table-row-selected' : ''}
          />
        </Spin>
      </Card>

      {!loading && adminModels.length > 0 && (
        <Card style={{ marginTop: 12 }} size="small">
          <Space>
            <Text type="secondary">Current model:</Text>
            <Text strong>{modelId || 'Not selected'}</Text>
            <Text type="secondary">
              · {adminModels.filter((m) => m.enabled).length} enabled / {adminModels.length} total
            </Text>
          </Space>
        </Card>
      )}

      <Modal
        title={
          <Space>
            <ApiOutlined />
            {editing ? `Edit: ${editing.modelId}` : 'Add Model'}
          </Space>
        }
        open={modalOpen}
        onOk={onSave}
        onCancel={() => setModalOpen(false)}
        confirmLoading={busy}
        okText="Save"
        cancelText="Cancel"
        width={560}
        destroyOnClose
      >
        <Space direction="vertical" style={{ width: '100%' }} size={12}>
          <Row gutter={12}>
            <Col span={12}>
              <Text style={{ fontSize: 12 }}>Model ID *</Text>
              <Input
                value={form.modelId}
                onChange={(e) => updateForm('modelId', e.target.value)}
                placeholder="e.g. deepseek-chat"
                disabled={!!editing}
                style={{ marginTop: 4 }}
              />
            </Col>
            <Col span={12}>
              <Text style={{ fontSize: 12 }}>Display Name</Text>
              <Input
                value={form.displayName}
                onChange={(e) => updateForm('displayName', e.target.value)}
                placeholder="e.g. DeepSeek Chat"
                style={{ marginTop: 4 }}
              />
            </Col>
          </Row>
          <Row gutter={12}>
            <Col span={12}>
              <Text style={{ fontSize: 12 }}>Provider</Text>
              <Select
                value={form.provider}
                onChange={(v) => updateForm('provider', v)}
                style={{ width: '100%', marginTop: 4 }}
                options={[
                  { value: 'openai', label: 'OpenAI Compatible' },
                  { value: 'deepseek', label: 'DeepSeek' },
                  { value: 'dashscope', label: 'DashScope' },
                  { value: 'ollama', label: 'Ollama' },
                  { value: 'other', label: 'Other' },
                ]}
              />
            </Col>
            <Col span={12}>
              <Text style={{ fontSize: 12 }}>Model Name *</Text>
              <Input
                value={form.modelName}
                onChange={(e) => updateForm('modelName', e.target.value)}
                placeholder="e.g. gpt-4o, deepseek-chat"
                style={{ marginTop: 4 }}
              />
            </Col>
          </Row>
          <div>
            <Text style={{ fontSize: 12 }}>Base URL *</Text>
            <Input
              value={form.baseUrl}
              onChange={(e) => updateForm('baseUrl', e.target.value)}
              placeholder="https://api.openai.com/v1"
              style={{ marginTop: 4 }}
            />
          </div>
          <div>
            <Text style={{ fontSize: 12 }}>API Key *</Text>
            <Input.Password
              value={form.apiKey}
              onChange={(e) => updateForm('apiKey', e.target.value)}
              placeholder="sk-..."
              style={{ marginTop: 4 }}
            />
          </div>
          <Row gutter={12}>
            <Col span={16}>
              <Text style={{ fontSize: 12 }}>Capabilities</Text>
              <div style={{ marginTop: 4 }}>
                <Space>
                  <Checkbox checked={form.capabilities.chat} onChange={(e) => updateCap('chat', e.target.checked)}>Chat</Checkbox>
                  <Checkbox checked={form.capabilities.tools} onChange={(e) => updateCap('tools', e.target.checked)}>Tools</Checkbox>
                  <Checkbox checked={form.capabilities.jsonMode} onChange={(e) => updateCap('jsonMode', e.target.checked)}>JSON</Checkbox>
                  <Checkbox checked={form.capabilities.vision} onChange={(e) => updateCap('vision', e.target.checked)}>Vision</Checkbox>
                </Space>
              </div>
            </Col>
            <Col span={8}>
              <Text style={{ fontSize: 12 }}>Sort Order</Text>
              <InputNumber
                value={form.sortOrder}
                onChange={(v) => updateForm('sortOrder', v)}
                min={0}
                style={{ width: '100%', marginTop: 4 }}
              />
            </Col>
          </Row>
        </Space>
      </Modal>
    </div>
  );
}

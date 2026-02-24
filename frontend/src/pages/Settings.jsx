import { useState, useEffect } from 'react';
import {
  Card, Form, Input, Button, Badge, Space, Typography, Row, Col, message, Divider,
} from 'antd';
import { CheckCircleFilled, ExclamationCircleFilled } from '@ant-design/icons';
import { getSettings, setSetting } from '../core/api/settings';

const { Title, Text, Paragraph } = Typography;

const PROVIDER_META = {
  'provider.dashscope.api-key': {
    name: 'DashScope (通义千问)',
    description: '阿里云通义千问大模型服务，支持 qwen-plus、qwen-max 等系列模型。',
    baseUrl: 'https://dashscope.aliyuncs.com/compatible-mode',
    placeholder: 'sk-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx',
    docsUrl: 'https://dashscope.console.aliyun.com/apiKey',
  },
  'provider.openai.api-key': {
    name: 'OpenAI',
    description: '使用 GPT-4o、GPT-4 Turbo 等 OpenAI 模型（需重启生效）。',
    baseUrl: 'https://api.openai.com',
    placeholder: 'sk-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx',
    docsUrl: 'https://platform.openai.com/api-keys',
  },
};

function ProviderCard({ settingKey, meta, initialConfigured }) {
  const [form] = Form.useForm();
  const [configured, setConfigured] = useState(initialConfigured);
  const [saving, setSaving] = useState(false);

  const handleSave = async () => {
    const values = await form.validateFields();
    setSaving(true);
    try {
      await setSetting(settingKey, values.apiKey);
      setConfigured(true);
      form.resetFields();
      message.success(`${meta.name} API Key 已保存`);
    } catch {
      message.error('保存失败，请重试');
    } finally {
      setSaving(false);
    }
  };

  const handleClear = async () => {
    setSaving(true);
    try {
      await setSetting(settingKey, '');
      setConfigured(false);
      form.resetFields();
      message.success(`${meta.name} API Key 已清除`);
    } catch {
      message.error('清除失败，请重试');
    } finally {
      setSaving(false);
    }
  };

  return (
    <Card
      style={{ marginBottom: 24 }}
      title={
        <Space>
          <Title level={5} style={{ margin: 0 }}>{meta.name}</Title>
          {configured ? (
            <Badge
              count={<CheckCircleFilled style={{ color: '#52c41a' }} />}
              style={{ marginLeft: 4 }}
            />
          ) : (
            <Badge
              count={<ExclamationCircleFilled style={{ color: '#faad14' }} />}
              style={{ marginLeft: 4 }}
            />
          )}
          <Text type={configured ? 'success' : 'warning'} style={{ fontSize: 12 }}>
            {configured ? '已配置' : '未配置'}
          </Text>
        </Space>
      }
    >
      <Paragraph type="secondary" style={{ marginBottom: 16 }}>{meta.description}</Paragraph>

      <Row gutter={[16, 0]} style={{ marginBottom: 8 }}>
        <Col span={4}>
          <Text type="secondary" style={{ lineHeight: '32px' }}>Base URL</Text>
        </Col>
        <Col span={20}>
          <Text code style={{ fontSize: 12 }}>{meta.baseUrl}</Text>
        </Col>
      </Row>

      <Divider style={{ margin: '12px 0' }} />

      <Form form={form} layout="vertical">
        <Form.Item
          name="apiKey"
          label="API Key"
          rules={[{ required: true, message: '请输入 API Key' }]}
          extra={
            <a href={meta.docsUrl} target="_blank" rel="noreferrer" style={{ fontSize: 12 }}>
              获取 API Key →
            </a>
          }
        >
          <Input.Password
            placeholder={configured ? '已配置（输入新值以更新）' : meta.placeholder}
            autoComplete="off"
          />
        </Form.Item>

        <Space>
          <Button type="primary" onClick={handleSave} loading={saving}>
            保存
          </Button>
          {configured && (
            <Button danger onClick={handleClear} loading={saving}>
              清除
            </Button>
          )}
        </Space>
      </Form>
    </Card>
  );
}

export default function Settings() {
  const [settings, setSettings] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    getSettings()
      .then(setSettings)
      .catch(() => message.error('加载配置失败'))
      .finally(() => setLoading(false));
  }, []);

  const getConfigured = (key) => {
    const entry = settings.find(s => s.key === key);
    return entry ? entry.configured : false;
  };

  return (
    <div style={{ maxWidth: 800 }}>
      <Title level={4} style={{ marginBottom: 4 }}>系统配置</Title>
      <Paragraph type="secondary" style={{ marginBottom: 24 }}>
        在此配置 AI 模型提供商的 API Key，无需重启即可生效（OpenAI 需重启）。
      </Paragraph>

      {!loading && Object.entries(PROVIDER_META).map(([key, meta]) => (
        <ProviderCard
          key={key}
          settingKey={key}
          meta={meta}
          initialConfigured={getConfigured(key)}
        />
      ))}
    </div>
  );
}

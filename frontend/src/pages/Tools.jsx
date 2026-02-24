import { useEffect, useState } from 'react';
import { Card, Table, Tag, Typography, Space, Spin, Alert, Switch } from 'antd';
import { ToolOutlined, WarningOutlined, CheckOutlined } from '@ant-design/icons';
import { fetchTools } from '../core/api/chat';
import { useChatStore } from '../core/state/chatStore';

const { Title, Text } = Typography;

const RISK_COLOR = { LOW: 'green', MEDIUM: 'orange', HIGH: 'red' };
const RISK_LABEL = { LOW: '低风险', MEDIUM: '中风险', HIGH: '高风险' };

export default function Tools() {
  const { selectedTools, setSelectedTools } = useChatStore();
  const [tools, setTools] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    fetchTools()
      .then(setTools)
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, []);

  const toggleTool = (toolName) => {
    setSelectedTools(
      selectedTools.includes(toolName)
        ? selectedTools.filter((t) => t !== toolName)
        : [...selectedTools, toolName]
    );
  };

  const columns = [
    {
      title: '启用',
      key: 'enabled',
      width: 80,
      render: (_, record) => (
        <Switch
          size="small"
          checked={selectedTools.includes(record.toolName)}
          onChange={() => toggleTool(record.toolName)}
        />
      ),
    },
    {
      title: '工具名称',
      dataIndex: 'toolName',
      key: 'toolName',
      render: (name) => (
        <Space>
          <ToolOutlined style={{ color: '#fa8c16' }} />
          <Text strong style={{ fontSize: 13 }}>{name}</Text>
        </Space>
      ),
    },
    {
      title: '描述',
      dataIndex: 'description',
      key: 'description',
      ellipsis: true,
      render: (desc) => <Text type="secondary">{desc || '—'}</Text>,
    },
    {
      title: '风险等级',
      dataIndex: 'riskLevel',
      key: 'riskLevel',
      width: 120,
      render: (level) => (
        <Tag color={RISK_COLOR[level] || 'default'} icon={level === 'HIGH' ? <WarningOutlined /> : undefined}>
          {RISK_LABEL[level] || level || '未知'}
        </Tag>
      ),
    },
  ];

  return (
    <div>
      <div style={{ marginBottom: 24 }}>
        <Title level={4} style={{ margin: 0 }}>工具管理</Title>
        <Text type="secondary">查看可用工具，在对话中启用或禁用工具</Text>
      </div>

      {error && <Alert type="error" message={error} showIcon style={{ marginBottom: 16 }} />}

      <Card
        extra={
          selectedTools.length > 0 && (
            <Tag color="blue">{selectedTools.length} 个工具已启用</Tag>
          )
        }
      >
        <Spin spinning={loading}>
          <Table
            dataSource={tools}
            columns={columns}
            rowKey="toolName"
            pagination={false}
            size="middle"
            locale={{ emptyText: loading ? '加载中...' : '暂无工具数据' }}
          />
        </Spin>
      </Card>

      {selectedTools.length > 0 && (
        <Card style={{ marginTop: 12 }} size="small">
          <Space wrap>
            <Text type="secondary">已启用工具:</Text>
            {selectedTools.map((t) => (
              <Tag key={t} closable onClose={() => toggleTool(t)} color="blue">{t}</Tag>
            ))}
          </Space>
        </Card>
      )}
    </div>
  );
}
